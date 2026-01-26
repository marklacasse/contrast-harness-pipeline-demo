# Harness CI/CD Setup Guide

This guide walks you through setting up the Harness pipeline for the Vulnerable Demo Application.

## Prerequisites

1. **Harness Account**
   - Sign up at https://app.harness.io
   - Create an organization and project

2. **AWS EKS Cluster**
   - Running Kubernetes cluster
   - kubectl access configured
   - Appropriate IAM permissions

3. **Docker Registry**
   - Docker Hub, AWS ECR, or other registry
   - Push/pull permissions

4. **Git Repository**
   - This code pushed to GitHub, GitLab, or Bitbucket

## Step 1: Configure Connectors

### Git Connector

1. Navigate to **Project Setup** → **Connectors**
2. Click **New Connector** → **Code Repositories** → **GitHub/GitLab**
3. Configure:
   - **Name**: `github-connector`
   - **URL**: Your repository URL
   - **Authentication**: SSH Key or Personal Access Token
4. Test and save

### Docker Registry Connector

1. Navigate to **Project Setup** → **Connectors**
2. Click **New Connector** → **Artifact Repositories** → **Docker Registry**
3. Configure:
   - **Name**: `docker-connector`
   - **Registry URL**: `https://index.docker.io/v2/` (for Docker Hub)
   - **Authentication**: Username/Password
4. Test and save

### AWS Cloud Provider (for EKS)

1. Navigate to **Project Setup** → **Connectors**
2. Click **New Connector** → **Cloud Providers** → **AWS**
3. Configure:
   - **Name**: `aws-connector`
   - **Authentication**: IAM Role or Access Key
   - **Test Region**: Your EKS region
4. Test and save

### Kubernetes Cluster Connector

1. Navigate to **Project Setup** → **Connectors**
2. Click **New Connector** → **Cloud Providers** → **Kubernetes**
3. Configure:
   - **Name**: `eks-connector`
   - **Details**: Delegate-based or Service Account
   - **Select Cloud Provider**: aws-connector
4. Test and save

## Step 2: Create Service

1. Navigate to **Deployments** → **Services**
2. Click **New Service**
3. Configure:
   - **Name**: `vulnerable-app`
   - **Deployment Type**: Kubernetes
4. Add Artifact:
   - **Artifact Repository**: docker-connector
   - **Artifact Path**: `<your-docker-repo>/vulnerable-app`
   - **Tag**: `<+pipeline.sequenceId>` or `latest`

## Step 3: Create Environment

1. Navigate to **Deployments** → **Environments**
2. Click **New Environment**
3. Configure:
   - **Name**: `eks-dev`
   - **Environment Type**: Non-Production
4. Add Infrastructure Definition:
   - **Name**: `eks-cluster`
   - **Deployment Type**: Kubernetes
   - **Cloud Provider**: eks-connector
   - **Namespace**: `default` or create new

## Step 4: Update Pipeline Configuration

Edit `.harness/pipeline.yaml` and replace placeholders:

```yaml
# Replace these values:
<YOUR_PROJECT_ID>     → Your Harness project identifier
<YOUR_ORG_ID>         → Your Harness organization identifier
<YOUR_DOCKER_CONNECTOR> → docker-connector
<YOUR_DOCKER_REPO>    → your-dockerhub-username
<YOUR_GIT_CONNECTOR>  → github-connector
```

Example:
```yaml
projectIdentifier: default_project
orgIdentifier: default
connectorRef: docker-connector
repo: johndoe/vulnerable-app
connectorRef: github-connector
```

## Step 5: Import Pipeline

### Option A: Via Git Sync

1. Navigate to **Pipelines**
2. Click **Create Pipeline** → **Import From Git**
3. Select your Git connector
4. Browse to `.harness/pipeline.yaml`
5. Click **Import**

### Option B: Manual Import

1. Navigate to **Pipelines**
2. Click **Create Pipeline**
3. Switch to **YAML** view
4. Copy contents of `.harness/pipeline.yaml`
5. Paste and save

## Step 6: Configure Pipeline Variables

Add pipeline variables for flexibility:

1. Open your pipeline
2. Go to **Variables** tab
3. Add:
   - `DOCKER_REPO`: Your Docker repository
   - `IMAGE_TAG`: `<+pipeline.sequenceId>`
   - `K8S_NAMESPACE`: Target namespace
   - `REPLICAS`: Number of replicas (default: 2)

## Step 7: Update Kubernetes Manifests

Update `k8s/deployment.yaml`:

```yaml
# Replace
image: <YOUR_DOCKER_REPO>/vulnerable-app:latest

# With Harness expression
image: <+artifact.image>
```

## Step 8: Test the Pipeline

1. Go to your pipeline
2. Click **Run**
3. Select:
   - **Git Branch**: main/master
   - **Artifact Tag**: latest or specific version
4. Click **Run Pipeline**

Monitor the execution:
- **Build Stage**: Maven build, tests, Docker build/push
- **Deploy Stage**: Kubernetes deployment, verification

## Step 9: Configure Triggers (Optional)

### Git Webhook Trigger

1. In your pipeline, go to **Triggers**
2. Click **New Trigger** → **Webhook**
3. Configure:
   - **Type**: GitHub/GitLab
   - **Event**: Push
   - **Branch**: main
4. Copy webhook URL
5. Add to your Git repository settings

### Scheduled Trigger

1. Click **New Trigger** → **Scheduled**
2. Configure CRON expression
3. Example: `0 2 * * *` (daily at 2 AM)

## Advanced Configuration

### Add Security Scanning

```yaml
- step:
    type: Security
    name: SAST Scan
    identifier: sast_scan
    spec:
      privileged: true
      settings:
        product_name: sonarqube
        scan_type: repository
        repository_project: vulnerable-app
```

### Add Approval Step

```yaml
- step:
    type: HarnessApproval
    name: Approve Deployment
    identifier: approve
    spec:
      approvalMessage: Please review and approve deployment
      includePipelineExecutionHistory: true
      approvers:
        userGroups:
          - account.admin
        minimumCount: 1
```

## Monitoring and Notifications

### Configure Slack Notifications

1. Create Slack connector
2. Add notification rules:
   - Pipeline Success
   - Pipeline Failure
   - Deployment Success

### Set Up Dashboards

1. Navigate to **Dashboards**
2. Create custom dashboard
3. Add widgets:
   - Pipeline Execution Trends
   - Deployment Frequency
   - Success Rate
   - MTTR (Mean Time To Recovery)

## Troubleshooting

### Pipeline Fails at Build Stage

Check:
- Maven is correctly configured
- Java version is 17
- Dependencies are accessible

### Pipeline Fails at Docker Push

Check:
- Docker connector credentials
- Registry permissions
- Network connectivity

### Deployment Fails

Check:
- Kubernetes connector configuration
- Namespace exists
- Resource quotas
- Image pull secrets

### Cannot Access Application

Check:
- LoadBalancer service created
- Security groups allow traffic
- DNS resolution
- Health checks passing

## Best Practices

1. **Use Secrets Management**
   - Store API keys in Harness Secrets
   - Never commit credentials

2. **Implement GitOps**
   - Store configs in Git
   - Use Git sync for pipelines

3. **Use Templates**
   - Create reusable step templates
   - Share across pipelines

4. **Enable Audit Trail**
   - Track all changes
   - Review deployment history

5. **Set Up Rollback**
   - Configure automatic rollback
   - Test rollback procedures

## Next Steps

1. ✅ Set up pipeline triggers
2. ✅ Configure notifications
3. ✅ Add approval workflows
4. ✅ Integrate security scanning
5. ✅ Set up monitoring
6. ✅ Document runbooks

## Resources

- [Harness Documentation](https://docs.harness.io/)
- [Kubernetes Deployment](https://developer.harness.io/docs/continuous-delivery/deploy-srv-diff-platforms/kubernetes/)
- [CI/CD Best Practices](https://developer.harness.io/docs/continuous-integration/get-started/ci-pipeline-basics/)
- [AWS EKS Guide](https://docs.aws.amazon.com/eks/)

## Support

For issues:
1. Check Harness documentation
2. Review pipeline execution logs
3. Contact Harness support
4. Check community forums

---

**Your pipeline is ready! Deploy with confidence! 🚀**
