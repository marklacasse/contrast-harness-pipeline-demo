# Harness Pipeline Setup Guide

This guide explains how to configure the complete CI/CD pipeline with Contrast Security integration.

## Pipeline Overview

The pipeline consists of 5 stages:

```
1. Build and Test → 2. Deploy to EKS → 3. Run Security Tests → 4. Cleanup → 5. Security Gate
```

### Stage Details

1. **Build and Test** (CI)
   - Maven build with Java 17
   - Unit tests
   - Docker image build and push to DockerHub

2. **Deploy to EKS** (Deployment)
   - Deploy to Kubernetes using Kustomize
   - Rolling deployment to production namespace
   - Contrast agent injected via K8s operator

3. **Run Security Tests** (CI)
   - Get LoadBalancer URL
   - Execute vulnerability tests (`tests/run-route-tests.sh`)
   - Triggers IAST analysis (25+ vulnerabilities)

4. **Cleanup Test Deployment** (CI)
   - Delete Deployment, Service, ConfigMap
   - Preserve namespace
   - Wait 60 seconds for Contrast processing

5. **Security Gate** (CI)
   - Trigger GitHub Action via `gh` CLI
   - Wait for verification results
   - Pass/Fail based on vulnerability threshold

## Prerequisites

### 1. Harness Setup

Create these in your Harness account:

**Connectors:**
- Docker Registry connector (DockerHub or other)
- Git connector (GitHub)
- Kubernetes cluster connector (EKS)

**Secrets:**
- `github_token` - GitHub PAT with `workflow` permissions
- `contrast_host` - Contrast TeamServer URL (for display only)

**Service:**
- Name: `vulnerable_app`
- Type: Kubernetes
- Manifests: Kustomize from repo

**Environment:**
- Name: `eks_prod`
- Type: Production
- Infrastructure: EKS cluster

### 2. GitHub Setup

Required repository secrets (see `.github/CONTRAST_SETUP.md`):
- `CONTRAST_API_KEY`
- `CONTRAST_AUTHORIZATION`
- `CONTRAST_ORGANIZATION_ID`
- `CONTRAST_HOST`

### 3. Kubernetes Setup

Required Contrast operator in cluster:
```bash
kubectl apply -f https://github.com/Contrast-Security-OSS/agent-operator/releases/latest/download/agent-operator.yaml
```

Apply Contrast agent configuration:
```bash
kubectl create secret generic contrast-agent-config \
  --from-literal=api.key=<YOUR_API_KEY> \
  --from-literal=api.service_key=<YOUR_SERVICE_KEY> \
  --from-literal=api.user_name=<YOUR_USERNAME> \
  --from-literal=api.url=https://teamserver-staging.contsec.com/Contrast \
  --namespace=production
```

Label namespace for agent injection:
```bash
kubectl label namespace production contrast-injection=enabled
```

## Pipeline Configuration

Update `.harness/pipeline.yaml` with your values:

### Required Replacements

```yaml
# Line 3-4: Project and Organization
projectIdentifier: <YOUR_PROJECT_ID>    # Replace with your project
orgIdentifier: <YOUR_ORG_ID>            # Replace with your org

# Line 30, 42, 54: Docker connector
connectorRef: <YOUR_DOCKER_CONNECTOR>   # Replace with your connector ID

# Line 56: Docker repository
repo: zencid/contrast-harness-pipeline-demo  # Update if using different registry

# Line 88: Environment reference
environmentRef: eks_prod                # Replace with your environment

# Line 140, 147: Kubernetes context
k8s_context: <YOUR_K8S_CONTEXT>        # Replace with your EKS context name

# Line 336: Git connector
connectorRef: <YOUR_GIT_CONNECTOR>      # Replace with your Git connector ID
```

### Optional Customizations

**Docker Image Tag Strategy:**
```yaml
# Use branch + build number
tags:
  - <+codebase.branch>-<+pipeline.sequenceId>
```

**Namespace:**
```yaml
# Change from 'production' to different namespace
namespace: staging
```

**Wait Times:**
```yaml
# Adjust Contrast processing wait time (line 211)
sleep 60  # Increase if needed for more findings
```

## Testing the Pipeline

### 1. Initial Test (Manual Trigger)

1. Go to Harness → Pipelines → "Vulnerable App CI/CD with Security Gate"
2. Click "Run" → Select branch → "Run Pipeline"
3. Monitor each stage:
   - Build: ~3-5 minutes
   - Deploy: ~2-3 minutes
   - Tests: ~1-2 minutes (100+ test cases)
   - Cleanup: ~1 minute
   - Security Gate: ~2 minutes

### 2. Expected Results

**Security Gate Should FAIL** (intentionally vulnerable app):
```
❌ Security verification FAILED!
Critical or high severity vulnerabilities were found.
```

**Vulnerabilities Expected:**
- SQL Injection: 3-5 instances
- XSS (Reflected): 4-6 instances
- Path Traversal: 2-3 instances
- Command Injection: 2-3 instances
- XXE: 1-2 instances
- Other OWASP Top 10: 10-15 instances

**Total Expected: 20-30 vulnerabilities**

### 3. Verify in Contrast

1. Log into Contrast TeamServer
2. Go to Applications → "contrast-harness-demo"
3. Verify vulnerabilities are present
4. Check route coverage (50 routes, all exercised)

## Troubleshooting

### Pipeline Fails at Build Stage

**Issue**: Maven build errors
**Solution**: 
```bash
# Test locally
mvn clean package -DskipTests
```

### Pipeline Fails at Deploy Stage

**Issue**: Kubernetes connection or namespace issues
**Solution**:
```bash
# Verify cluster access
kubectl get nodes
kubectl get namespaces
kubectl get all -n production
```

### Tests Don't Trigger Vulnerabilities

**Issue**: Wrong service URL or tests not running
**Solution**:
```bash
# Check LoadBalancer is ready
kubectl get svc vulnerable-app -n production

# Test manually
SERVICE_URL=$(kubectl get svc vulnerable-app -n production -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
./tests/run-route-tests.sh "http://${SERVICE_URL}:8080"
```

### Security Gate Never Fails

**Issue**: Tests not reaching application or agent not working
**Solution**:
```bash
# Check agent logs
kubectl logs -n production -l app=vulnerable-app -c vulnerable-app

# Verify agent injection
kubectl describe pod -n production -l app=vulnerable-app | grep contrast

# Check Contrast dashboard for activity
```

### GitHub Action Fails

**Issue**: Authentication or secrets not configured
**Solution**:
- Verify all 4 GitHub secrets are set correctly
- Check `GITHUB_TOKEN` secret in Harness has `workflow` permission
- Review `.github/workflows/contrast-verify.yml` parameters

### Cleanup Stage Issues

**Issue**: Resources not deleted
**Solution**:
```bash
# Manual cleanup
kubectl delete deployment vulnerable-app -n production
kubectl delete service vulnerable-app -n production
kubectl delete configmap vulnerable-app-config -n production
```

## Pipeline Outputs

### Success Path (No Vulnerabilities)

```
✅ SECURITY GATE PASSED
Build: 123
Application: contrast-harness-demo
Status: READY FOR PRODUCTION
```

### Failure Path (Vulnerabilities Found)

```
❌ Security verification FAILED!
Critical or high severity vulnerabilities were found.
Review Contrast dashboard: https://teamserver-staging.contsec.com
```

## Next Steps

1. **Fix Vulnerabilities**: This is a demo app - vulnerabilities are intentional
2. **Add Production Deployment**: Add stage 6 that deploys only if security gate passes
3. **Customize Thresholds**: Modify `.github/workflows/contrast-verify.yml` to allow some vulnerabilities
4. **Add Notifications**: Configure Slack/email alerts on security gate failures
5. **Branch Protection**: Require security gate to pass before merging PRs

## Advanced Configuration

### Parallel Testing

Run tests in parallel for faster execution:
```yaml
- step:
    type: Run
    name: Run Vuln Tests in Parallel
    strategy:
      parallelism: 5
```

### Dynamic Wait Times

Calculate wait time based on route count:
```yaml
ROUTE_COUNT=$(curl -s ${SERVICE_URL}/actuator/mappings | jq '.contexts[].mappings | length')
WAIT_TIME=$((ROUTE_COUNT * 2))  # 2 seconds per route
sleep ${WAIT_TIME}
```

### Conditional Security Gate

Only enforce for production:
```yaml
when:
  condition: <+pipeline.variables.environment> == "production"
```

## References

- [Harness Documentation](https://docs.harness.io/)
- [Contrast IAST Documentation](https://docs.contrastsecurity.com/)
- [GitHub Action Setup](.github/CONTRAST_SETUP.md)
- [Test Suite Documentation](tests/README.md)
