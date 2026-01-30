# GitHub Action Setup for Contrast Security Verification

This document explains how to set up the Contrast Security verification GitHub Action for automated security gating.

## Overview

The Contrast Security verification workflow should run **after** your application has been deployed and tested. The workflow:
1. Waits for Contrast IAST to process findings (configurable delay, default 60 seconds)
2. Queries Contrast for vulnerabilities in the deployed application
3. Fails the pipeline if critical or high severity vulnerabilities are found
4. Returns status and results for pipeline gating decisions

### Pipeline Timing

```
Deploy → Run Tests → Wait (60s) → Check Contrast → Gate Promotion
```

**Important**: This action should be the **last step** before production promotion:
- ✅ After application deployment to test/staging environment
- ✅ After all integration/functional tests have run
- ✅ After sufficient time for IAST analysis (minimum 60 seconds)
- ✅ Before production deployment approval

## Prerequisites

1. Access to your Contrast Security account
2. Admin access to this GitHub repository (to create secrets)

## Required GitHub Secrets

You need to create the following secrets in your GitHub repository:

### Navigate to: Repository Settings → Secrets and variables → Actions → New repository secret

Create these 4 secrets:

### 1. `CONTRAST_API_KEY`
- **Description**: Your Contrast API key
- **How to get it**:
  1. Log into Contrast Security
  2. Go to User Settings (top right) → Your Account
  3. Navigate to "API Key" section
  4. Copy your API Key

### 2. `CONTRAST_AUTHORIZATION`
- **Description**: Your Contrast authorization header (base64 encoded API key)
- **How to get it**:
  1. In Contrast, go to User Settings → Your Account
  2. Find "Authorization Header" or "Service Key"
  3. Copy the entire authorization header value
  4. Format: `<base64-encoded-value>`

### 3. `CONTRAST_ORGANIZATION_ID`
- **Description**: Your Contrast organization ID
- **How to get it**:
  1. In Contrast, click on your organization name (top right)
  2. Go to Organization Settings
  3. Copy the Organization ID (UUID format)
  4. Example: `12345678-1234-1234-1234-123456789012`

### 4. `CONTRAST_HOST`
- **Description**: Your Contrast host URL
- **Value**: 
  - Production: `https://app.contrastsecurity.com/Contrast/api`
  - Staging: `https://teamserver-staging.contsec.com/Contrast/api`
  - EU: `https://app.contrastsecurity.eu/Contrast/api`
  - Your local installation URL if applicable

## Quick Setup Guide

### Step 1: Find Your Contrast API Credentials

1. Log into Contrast Security: https://app.contrastsecurity.com (or your instance)
2. Click your username (top right) → **Your Account**
3. Scroll to **API Key** section - you'll see:
   - Organization ID
   - API Key
   - Authorization Header (Service Key)

### Step 2: Create GitHub Secrets

```bash
# Navigate to your GitHub repository
https://github.com/marklacasse/contrast-harness-pipeline-demo/settings/secrets/actions

# Click "New repository secret" and add each of the 4 secrets above
```

### Step 3: Test the Workflow

You can manually trigger the workflow to test it:

1. Go to **Actions** tab in GitHub
2. Select **Contrast Security Verification** workflow
3. Click **Run workflow**
4. Fill in:
   - **app-name**: `contrast-harness-demo` (production) or `contrast-harness-pipeline-test` (local)
   - **build-number**: (optional) Your Harness build number from pipeline
   - **branch-name**: (optional) Filter by branch like `main`
5. Click **Run workflow**

## Workflow Behavior

### What It Does

1. **Waits 60 seconds** for Contrast to process vulnerabilities from your tests
2. **Queries Contrast API** for vulnerabilities in the specified application
3. **Filters by severity**: Only checks for `critical` and `high` vulnerabilities
4. **Time window**: Only looks at vulnerabilities found in last 2 hours (7200 seconds)
5. **Fails the workflow** if any critical/high vulnerabilities are found
6. **Passes** if no critical/high vulnerabilities are present

### Severity Thresholds

Currently configured to **fail on HIGH or CRITICAL**:
```yaml
fail-threshold: high
severities: critical,high
```

You can adjust this to be more/less strict:
- `fail-threshold: critical` - Only fail on critical (allows high)
- `fail-threshold: medium` - Fail on medium and above
- `severities: critical,high,medium` - Check more severity levels

### Filtering by Build/Branch

If you pass build-number or branch-name, the action will filter vulnerabilities to only those found in that specific build/branch:

```yaml
build-number: "15"  # Matches CONTRAST__APPLICATION__SESSION_METADATA from Harness
branch-name: "main"  # Matches branch metadata
```

## Integration with Harness Pipeline

To call this GitHub Action from your Harness pipeline, add a step after running tests:

```yaml
- step:
    type: Run
    name: Trigger Security Gate
    identifier: security_gate
    spec:
      shell: Bash
      command: |
        # Trigger GitHub Action workflow
        curl -X POST \
          -H "Accept: application/vnd.github.v3+json" \
          -H "Authorization: token $GITHUB_TOKEN" \
          https://api.github.com/repos/marklacasse/contrast-harness-pipeline-demo/actions/workflows/contrast-verify.yml/dispatches \
          -d '{"ref":"main","inputs":{"app-name":"contrast-harness-demo","build-number":"<+pipeline.sequenceId>"}}'
        
        # Wait for workflow to complete
        sleep 90
        
        # Check workflow status (implement polling logic)
        # Fail pipeline if security gate fails
```

Alternatively, you can use Harness's built-in GitHub integration to trigger the workflow and wait for completion.

## Viewing Results

### In GitHub Actions
1. Go to **Actions** tab
2. Click on the workflow run
3. View the **Contrast Security Verification** step
4. See pass/fail status and any error messages

### In Contrast Dashboard
1. Navigate to your application
2. View **Vulnerabilities** tab
3. Filter by severity, build number, or branch
4. Review findings and remediation guidance

## Troubleshooting

### Error: "Invalid API credentials"
- Double-check all 4 secrets are set correctly
- Verify API key hasn't expired
- Ensure authorization header includes the base64 encoded value

### Error: "Application not found"
- Verify the application name exactly matches what's in Contrast
- Check that the app has reported to Contrast recently (status: online)
- Application names are case-sensitive

### Error: "No vulnerabilities found but expected some"
- Increase the `age-threshold` (currently 7200 seconds = 2 hours)
- Verify tests actually ran and exercised vulnerable routes
- Check Contrast agent is properly attached to the application

### Workflow passes but should fail
- Check the `fail-threshold` and `severities` settings
- Verify the time window (`age-threshold`) is correct
- Confirm build-number/branch-name filters aren't excluding vulnerabilities

## Advanced Configuration

### Calling from Harness Pipeline

To integrate this GitHub Action into your Harness pipeline, add a step after your deployment and tests:

```yaml
# Harness Pipeline Stage: Security Gate
- step:
    type: Run
    name: Trigger Security Verification
    identifier: trigger_security_gate
    spec:
      shell: Bash
      command: |
        # Trigger GitHub Action workflow
        gh workflow run contrast-verify.yml \
          --repo marklacasse/contrast-harness-pipeline-demo \
          --ref master \
          -f app-name="contrast-harness-demo" \
          -f build-number="${HARNESS_BUILD_ID}" \
          -f wait-time="60"
        
        # Wait for workflow to start
        sleep 5
        
        # Get latest run ID
        RUN_ID=$(gh run list --workflow=contrast-verify.yml --limit 1 --json databaseId -q '.[0].databaseId')
        
        # Wait for workflow completion and get result
        gh run watch ${RUN_ID} --exit-status
      envVariables:
        GITHUB_TOKEN: <+secrets.getValue("github_token")>
```

**Note**: Requires GitHub CLI (`gh`) and a GitHub personal access token with `workflow` permissions.

### Custom Severity Rules

Create different workflows for different environments:

**Production Gate** (strict):
```yaml
fail-threshold: medium
severities: critical,high,medium
```

**Development Gate** (lenient):
```yaml
fail-threshold: critical
severities: critical
```

### Integration with Pull Requests

Trigger on PRs to gate merges:

```yaml
on:
  pull_request:
    branches: [ main ]
```

### Notifications

Add Slack/email notifications on failure:

```yaml
- name: Notify on Failure
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "Security gate failed! Critical vulnerabilities found."
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

## Security Best Practices

1. **Never commit API keys** - Always use GitHub secrets
2. **Rotate credentials regularly** - Update API keys every 90 days
3. **Use service accounts** - Create dedicated Contrast service user for CI/CD
4. **Limit permissions** - Service account only needs read access to vulnerabilities
5. **Audit workflow runs** - Review GitHub Actions logs regularly

## References

- [Contrast GitHub Action Documentation](https://github.com/Contrast-Security-OSS/integration-verify-github-action)
- [Contrast API Documentation](https://docs.contrastsecurity.com/en/api.html)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
