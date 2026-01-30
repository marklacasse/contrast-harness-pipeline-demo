# Harness Pipeline Guide

## Overview

This pipeline demonstrates a complete CI/CD workflow with Contrast Security integration, including:
- Build and test with Maven
- Docker image creation
- Kubernetes deployment
- Automated vulnerability testing
- Security verification gates with separate thresholds

## Pipeline Architecture

```
Build → Deploy → Run Tests → Verify Security
```

### Stage 1: Build and Test
- Maven build (`mvn clean package`)
- Unit tests (`mvn test`)
- Docker image build and push
- Tags: `latest` and `<build-number>`

### Stage 2: Deploy to EKS
- Kubernetes rolling deployment
- Uses Kustomize for environment configuration
- Injects Contrast agent configuration via environment variables
- Applies session metadata (branch name, build number)

### Stage 3: Run Vulnerability Tests
- Executes comprehensive test suite (`tests/run-route-tests.sh`)
- Tests 25+ vulnerability types across all endpoints
- Triggers IAST analysis with Contrast agent

### Stage 4: Verify Security Findings
Three steps:
1. **Close Agent Session** - Triggers Contrast analysis and auto-remediation
2. **Wait for Processing** - Gives Contrast 60 seconds to process findings
3. **Verify with Contrast API** - Checks vulnerability counts against thresholds

## Pipeline Variables

Configure these when running the pipeline:

| Variable | Description | Default |
|----------|-------------|---------|
| `criticalThreshold` | Max CRITICAL vulnerabilities allowed | 0 |
| `highThreshold` | Max HIGH vulnerabilities allowed | 5 |
| `dockerRepo` | Docker registry repository | zencid/contrast-harness-pipeline-demo |
| `namespace` | Kubernetes namespace | apps |
| `replicas` | Number of pod replicas | 2 |
| `appName` | Contrast application name | contrast-harness-demo |
| `tags` | Contrast application tags | demo |

## Security Gate Configuration

The pipeline makes **two separate API calls** to Contrast to check vulnerabilities by severity:

### Critical Vulnerabilities Check
```bash
curl -X GET "${API_HOST}/Contrast/api/ng/${ORG_ID}/traces/${APP_ID}/quick?severities=CRITICAL&appVersionTags=${BUILD_NUM}"
```

Extracts count and compares: `CRITICAL_COUNT > criticalThreshold` → Fail

### High Vulnerabilities Check
```bash
curl -X GET "${API_HOST}/Contrast/api/ng/${ORG_ID}/traces/${APP_ID}/quick?severities=HIGH&appVersionTags=${BUILD_NUM}"
```

Extracts count and compares: `HIGH_COUNT > highThreshold` → Fail

### Pass/Fail Logic
- Pipeline **FAILS** if EITHER threshold is exceeded
- Pipeline **PASSES** only if BOTH thresholds are satisfied
- Output shows: `CRITICAL: 0/0, HIGH: 3/5` (example)

## Required Secrets

Configure these in Harness Project Secrets:

| Secret | Description |
|--------|-------------|
| `CONTRAST_API_KEY` | Contrast API key |
| `CONTRAST_ORGANIZATION_ID` | Contrast organization UUID |
| `CONTRAST_AUTHORIZATION` | Base64 encoded username:service_key |
| `CONTRAST_HOST` | Contrast API host (e.g., https://eval.contrastsecurity.com) |

### Creating Contrast Authorization Header
```bash
echo -n "username:service_key" | base64
```

## Contrast Agent Configuration

The agent is configured via Kubernetes environment variables in `k8s-kustomize/patches/env-vars-patch.yaml`:

```yaml
env:
  - name: CONTRAST__APPLICATION__NAME
    value: <+serviceVariables.appName>
  - name: CONTRAST__APPLICATION__TAGS
    value: <+serviceVariables.tags>
  - name: CONTRAST__APPLICATION__SESSION_METADATA
    value: branchName=<+codebase.branch>,buildNumber=<+pipeline.sequenceId>
  - name: CONTRAST__APPLICATION__VERSION
    value: <+pipeline.sequenceId>
```

**Dynamic Variables:**
- `<+codebase.branch>` - Actual git branch being built
- `<+pipeline.sequenceId>` - Build number from Harness

This ensures each build is tracked separately in Contrast with correct metadata.

## Agent Session Management

### Opening Session
Agent starts automatically when application starts in pod. Session opens on first HTTP request.

### Closing Session
Pipeline explicitly closes session after tests:

```bash
curl -X POST "${API_HOST}/Contrast/api/ng/organizations/${ORG_ID}/agent-sessions/sbav" \
  -H "Authorization: ${AUTH_HEADER}" \
  -H "API-Key: ${API_KEY}" \
  -d '{
    "appName": "contrast-harness-demo",
    "appLanguage": "JAVA",
    "metadata": [
      {"label": "branchName", "value": "main"},
      {"label": "buildNumber", "value": "123"}
    ]
  }'
```

**Why close session?**
- Triggers Contrast to analyze observations
- Enables auto-remediation based on security controls
- Ensures findings are ready for verification step

### Wait Period
After closing session, pipeline waits 60 seconds for Contrast processing:
```bash
sleep 60
```

This ensures the `/quick` API returns complete results.

## Verification Step Details

The verification step queries Contrast API and parses vulnerability counts:

```bash
# Get app ID from name
APP_ID=$(curl "${API_HOST}/Contrast/api/ng/${ORG_ID}/applications/name?filterText=${APP_NAME}" | grep app_id)

# Query critical vulnerabilities
CRITICAL_COUNT=$(curl "${API_HOST}/Contrast/api/ng/${ORG_ID}/traces/${APP_ID}/quick?severities=CRITICAL&appVersionTags=${BUILD_NUM}" | grep count)

# Query high vulnerabilities  
HIGH_COUNT=$(curl "${API_HOST}/Contrast/api/ng/${ORG_ID}/traces/${APP_ID}/quick?severities=HIGH&appVersionTags=${BUILD_NUM}" | grep count)

# Check thresholds
if [ "$CRITICAL_COUNT" -gt "$CRITICAL_THRESHOLD" ]; then
  echo "❌ FAILED: CRITICAL: ${CRITICAL_COUNT}/${CRITICAL_THRESHOLD}"
  exit 1
fi

if [ "$HIGH_COUNT" -gt "$HIGH_THRESHOLD" ]; then
  echo "❌ FAILED: HIGH: ${HIGH_COUNT}/${HIGH_THRESHOLD}"
  exit 1
fi

echo "✓ PASSED: CRITICAL: ${CRITICAL_COUNT}/${CRITICAL_THRESHOLD}, HIGH: ${HIGH_COUNT}/${HIGH_THRESHOLD}"
```

### Output Variables
The step exports these for downstream stages:
- `CRITICAL_COUNT` - Number of critical vulnerabilities found
- `HIGH_COUNT` - Number of high vulnerabilities found
- `CRITICAL_THRESHOLD` - Threshold used
- `HIGH_THRESHOLD` - Threshold used
- `FAILURE_REASON` - Details if gate fails

## Running the Pipeline

1. **From Harness UI:**
   - Navigate to Pipelines → Vulnerable App CICD example
   - Click "Run"
   - Select branch (defaults to main)
   - Set thresholds (optional, uses defaults)
   - Click "Run Pipeline"

2. **From CLI/API:**
```bash
curl -X POST "https://app.harness.io/gateway/pipeline/api/pipeline/execute/..." \
  -H "x-api-key: ${HARNESS_API_KEY}" \
  -d '{
    "identifier": "Vulnerable_App_CICD_example",
    "inputSetYaml": "pipeline:\n  variables:\n    - name: criticalThreshold\n      value: 0\n    - name: highThreshold\n      value: 5"
  }'
```

## Troubleshooting

### Issue: Wrong branch name in Contrast metadata
**Cause:** Hardcoded branch in configuration  
**Fix:** Ensure using `<+codebase.branch>` dynamic variable

### Issue: Pipeline fails with "Application not found"
**Cause:** App name doesn't match Contrast  
**Fix:** Check `appName` variable matches exactly

### Issue: Vulnerabilities not detected
**Cause:** Tests didn't run or agent not active  
**Fix:** Check "Run Vulnerability tests" step logs

### Issue: Verification fails immediately
**Cause:** Didn't wait for Contrast processing  
**Fix:** Increase wait time from 60 to 90 seconds

### Issue: All vulnerabilities shown, even remediated
**Cause:** Using wrong `timestampFilter` parameter  
**Fix:** Ensure using `timestampFilter=FIRST` in API query

## Best Practices

1. **Threshold Strategy:**
   - CRITICAL: Always 0 (zero tolerance)
   - HIGH: 5-10 (allow managed technical debt)
   - Adjust based on team velocity and risk tolerance

2. **Branch Strategy:**
   - Run on every PR branch
   - Track vulnerabilities per branch
   - Use branch metadata for filtering in Contrast

3. **Build Numbers:**
   - Use sequential build numbers
   - Tag Docker images with build number
   - Use same number for Contrast version tracking

4. **Session Management:**
   - Always close agent session
   - Wait adequate time for processing
   - Use consistent metadata labels

5. **Security Controls:**
   - Integrate before vulnerable operations
   - Deploy with agent to enable auto-remediation
   - Verify controls in stack traces

## Pipeline File Location

`.harness/pipeline.yaml` - Complete pipeline definition

Key sections:
- Lines 1-70: Build stage
- Lines 71-130: Deploy stage  
- Lines 131-220: Test stage
- Lines 221-460: Verify stage with gate logic
- Lines 461-494: Variables
