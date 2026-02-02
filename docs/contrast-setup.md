# Contrast Security Setup

## Overview

This guide covers Contrast Security integration for the Harness CI/CD pipeline, focusing on:
- **Application configuration** via Kubernetes manifests (this repo)
- **API credentials** for pipeline verification security gates
- **Local dev testing** with agent credentials

**Note**: Agent authentication credentials are handled by the Contrast Kubernetes Operator installed via Helm (not covered here). This repo configures the application-level settings (name, tags, metadata) via Kustomize.

## Contrast Agent Configuration

The agent is configured via environment variables in the Kubernetes deployment. See `k8s-kustomize/patches/env-vars-patch.yaml`:

```yaml
env:
  - name: CONTRAST__APPLICATION__NAME
    value: contrast-harness-demo
  - name: CONTRAST__APPLICATION__TAGS
    value: demo
  - name: CONTRAST__APPLICATION__SESSION_METADATA
    value: branchName=<+codebase.branch>,buildNumber=<+pipeline.sequenceId>
  - name: CONTRAST__APPLICATION__VERSION
    value: <+pipeline.sequenceId>
```

### Configuration Details

**Application Name**: `CONTRAST__APPLICATION__NAME`
- Must match exactly in Contrast UI and pipeline verification
- Example: `contrast-harness-demo`

**Application Tags**: `CONTRAST__APPLICATION__TAGS`
- Comma-separated tags for organizing applications
- Used for filtering in Contrast UI
- Example: `demo,ci-cd,testing`

**Session Metadata**: `CONTRAST__APPLICATION__SESSION_METADATA`
- Key-value pairs for tracking builds
- Format: `key1=value1,key2=value2`
- Used to filter vulnerabilities by build/branch
- Dynamic variables:
  - `<+codebase.branch>` - Git branch being built
  - `<+pipeline.sequenceId>` - Harness build number

**Application Version**: `CONTRAST__APPLICATION__VERSION`
- Version identifier for the deployment
- Typically set to build number
- Used for gating pipelines in the Harness platform based on vulnerability thresholds

## Contrast Credentials Overview

Contrast requires **two separate sets of credentials** that serve different purposes and **must not be mixed**:

### 1. Agent Credentials (Runtime Authentication)
**Purpose**: Used by the Contrast agent running with your application to authenticate and send vulnerability data to the Contrast SaaS platform.

**Used by**: The agent embedded in your application runtime
**Configured in this project**: ❌ No - Handled by Contrast Kubernetes Operator via Helm
**Used for local testing**: ✅ Yes - Via `.contrast/contrast.yaml` file

**Authentication method**: Agent token (newer) or Agent Keys (legacy)
**Permissions**: Controlled account for agent-to-platform communication

**Best Practice**: Create a dedicated agent token for the Kubernetes Operator.

**Documentation**: 
- [Agent Configuration Settings](https://docs.contrastsecurity.com/en/configuration-settings.html)
- [Finding Agent Keys](https://docs.contrastsecurity.com/en/find-the-agent-keys.html)

### 2. API/Service User Credentials (Verification Authentication)
**Purpose**: Used by the pipeline to query the Contrast API for vulnerability verification and security gates.

**Used by**: Harness pipeline verification steps (API calls)
**Configured in this project**: ✅ Yes - As Harness secrets (see Harness Pipeline Guide)
**Used for local testing**: ❌ No - Not needed for local agent testing

**Authentication method**: User API key + Authorization header
**Permissions**: Service user with RBAC limited to view access for vulnerabilities

**Best Practice**: Create a dedicated service user in Contrast with minimal required permissions (view vulnerabilities only).

---

## Getting Agent Credentials (For Local Testing Only)

**Note**: In the Kubernetes/Harness pipeline, agent authentication is handled by the Contrast Operator. These steps are only needed for local development testing.

### Creating an Agent Token (Recommended)

1. Log into Contrast: https://app.contrastsecurity.com (or your instance)
2. Click organization name (top right) → **Organization Settings**
3. Navigate to **Agent Keys** section
4. Click **Add key name**
5. Enter key name (e.g., `local-dev-testing`) → click **Save key name**
6. Copy the generated token
7. Use this token in your local `.contrast/contrast.yaml` file

### Getting Agent Keys (Legacy Method)

1. Log into Contrast:
2. Click organization name (top right) → **Organization Settings**
3. Navigate to **Agent Keys** section
4. Click **Add key name**
5. Enter key name (e.g., `local-dev-testing`) → click **Save key name**
6. Expand **Legacy Agent Keys**
7. Copy:
   - **Agent Key Name(API username)**
   - **Agent Service Key**
   - **Agent API Key**
   - **Contrast Agent URL**
8. Use this token in your local `.contrast/contrast.yaml` file

### Agent Configuration in Kubernetes (Reference Only)

**In this project**, agent authentication is configured by the Contrast Kubernetes Operator (installed via Helm, outside this repo).

The operator handles:
- Agent credentials injection
- Agent attachment to pods
- Runtime instrumentation

**This project configures** (via Kustomize):
- Application name, tags, metadata
- Session metadata (build/branch tracking)
- Application version

See `k8s-kustomize/patches/env-vars-patch.yaml` for application configuration.

## Getting API Credentials (Pipeline Verification)

For configuring Harness pipeline verification steps (see [Harness Pipeline Guide](./harness-pipeline.md#required-secrets) for setup):

### Creating a Service User (Recommended)

1. Log into Contrast as an admin
2. Refer to [Contrast documentation](https://docs.contrastsecurity.com/en/create-api-only-user.html) for detailed instructions on creating service users.
3. **RBAC Configuration**:
   - Grant **View** access to specific applications
   - Grant **View Vulnerabilities** permission
   - Do NOT grant edit/delete permissions


### Retrieving API Credentials

1. Hover over the API only  label next to the user's name to copy the displayed Service key
2. The username (email used for the service user) and the Service key are unique to each service user. The rest of the authentication and connection detials are org wide. # For more information on API credentials and usage, refer to the official documentation.

#### Organization ID
- UUID format
- Example: `12345678-1234-1234-1234-123456789012`
- **Used by**: Both agent and API calls (can overlap)

#### API Key
- User-specific API key (NOT the agent API key)
- Example: `abc123def456...`
- **Used by**: Pipeline verification API calls

#### Authorization Header
- Base64 encoded `username:service_key`
- Example: `dXNlcm5hbWU6c2VydmljZV9rZXk=`
- **Used by**: Pipeline verification API calls

**To create manually**:
```bash
echo -n "service_username:service_key" | base64
```

#### API Host
- **SaaS**: `https://app.contrastsecurity.com`
  - **Used by**: Both agent and API calls (can overlap)
- **SaaS Agent Exclusive Endpoint** `https://app-agents.contrastsecurity.com`
  - **Used by**: Agent only

**Important**: Do NOT include `/Contrast/api` suffix - the pipeline adds this automatically.

### Configuring in Harness

Add these as Harness Project Secrets (API User credentials, not agent credentials):
- `CONTRAST_API_KEY` - Service user API key
- `CONTRAST_AUTHORIZATION` - Service user authorization header
- `CONTRAST_ORGANIZATION_ID` - Organization UUID (shared with agent)
- `CONTRAST_HOST` - Contrast instance URL (shared with agent)

## Credentials Summary Table

| Credential | Agent (Runtime) | API (Verification) | Shared Across Users? |
|------------|----------------|-------------------|---------------------|
| Organization ID | ✅ Required | ✅ Required | ✅ Yes - Org-wide |
| API Host/URL | ✅ Required | ✅ Required | ✅ Yes - Org-wide |
| API Key | ✅ Required (legacy) | ✅ Required | ✅ Yes - Org-wide |
| Agent Token | ✅ Required | ❌ Not used | ⚠️ Per token |
| Agent Service Key | ✅ Required (legacy) | ❌ Not used | ❌ No - Per user |
| Agent Username | ✅ Required (legacy) | ❌ Not used | ❌ No - Per user |
| Service User Service Key | ❌ Not used | ✅ Required | ❌ No - Per user |
| Service Username | ❌ Not used | ✅ Required | ❌ No - Per user |
| Authorization Header | ❌ Not used | ✅ Required | ❌ No - Per user (username:service_key) |

**Shared Credentials** (Org-wide):
- Organization ID, API Host/URL, API Key - Same for all users in the organization

**User-Specific Credentials** (Per-user):
- Username (agent or service user), Service Key (agent or service user)
- Authorization Header - base64 encoded `username:service_key`

**Warning**: Do NOT use agent credentials for API calls or vice versa. They serve different purposes and have different permission models.

## Local Testing with Contrast Agent

### Prerequisites
1. Download Contrast agent JAR from Maven repository
2. Obtain agent credentials (token or keys) from Contrast
3. Create `.contrast/contrast.yaml` configuration file

### Running Locally

```bash
# Start application with agent
java -javaagent:./.contrast/contrast-agent-6.25.1.jar \
     -Dcontrast.config.path=./.contrast/contrast.yaml \
     -jar target/vulnerable-app-1.0.0.jar

# Run vulnerability tests
./tests/run-route-tests.sh http://localhost:8080

# Check Contrast UI for findings
```

### Local Configuration File

Create `.contrast/contrast.yaml` with **agent credentials** (not API credentials):

**Option 1: Using Agent Token (Recommended)**
```yaml
api:
  url: https://app.contrastsecurity.com/Contrast
  agent_token: your-agent-token-here
  
application:
  name: contrast-harness-demo-local
  tags: local,testing
  session_metadata: developer=${USER}
  
server:
  environment: development
```

**Option 2: Using Agent Keys (Legacy)**
```yaml
api:
  url: https://app.contrastsecurity.com/Contrast
  api_key: <AGENT_API_KEY>
  service_key: <AGENT_SERVICE_KEY>
  user_name: <AGENT_USERNAME>
  
application:
  name: contrast-harness-demo-local
  tags: local,testing
  session_metadata: committer=${USER}
  
server:
  environment: development
```

## Viewing Results in Contrast UI

### Application Dashboard
1. Navigate to **Applications**
2. Find your application (e.g., `contrast-harness-demo`)
3. View current status and vulnerability counts

### Vulnerabilities
1. Click **Vulnerabilities** tab
2. Filter by:
   - **Severity**: Critical, High, Medium, Low
   - **Build**: Use build number from session metadata
   - **Branch**: Filter by branch name
   - **Status**: Open, Reported, Remediated

### Build Tracking
1. Use **Session Metadata** filters
2. Example: `buildNumber:58` shows only build 58 vulnerabilities
3. Compare builds to see trends

### Stack Traces
1. Click individual vulnerability
2. View **Details**
3. Check **Stack Trace** for data flow
4. Look for SecurityControls methods (if integrated)

## Security Controls Integration

After implementing custom security controls (see [Security Controls Guide](./security-controls.md)):

1. **Deploy with controls** - Code must call SecurityControls methods
2. **Trigger vulnerabilities** - Run test suite to exercise code paths
3. **Close agent session** - Pipeline closes session for processing
4. **Check auto-remediation** - Contrast recognizes controls and remediates

Example: Command Injection with security control visible in stack trace:
```
com.contrast.demo.controller.InjectionController.commandInjection
  → com.contrast.demo.security.SecurityControls.isSafeCommandInput ✓
  → Runtime.getRuntime().exec()
```

Contrast sees the security control and may auto-remediate the vulnerability.

## Troubleshooting

### Agent Not Reporting
- Check application started successfully
- Verify agent JAR path is correct
- Check `.contrast/contrast.yaml` credentials
- Ensure network connectivity to Contrast host

### Wrong Application Name
- Application name must match exactly (case-sensitive)
- Check environment variable: `CONTRAST__APPLICATION__NAME`
- Look in Contrast UI → Applications for actual name

### Vulnerabilities Not Filtering by Build
- Verify session metadata includes `buildNumber`
- Check format: `buildNumber=123` (no spaces)
- Ensure using correct filter syntax in Contrast UI

### Auto-Remediation Not Working
- Confirm SecurityControls called in code path
- Check stack traces show security control methods
- Verify agent session closed via API call
- Wait 60+ seconds after session close

## Best Practices

1. **Consistent naming** - Use same application name across environments
2. **Meaningful tags** - Tag by team, project, environment
3. **Session metadata** - Always include build number and branch
4. **Version tracking** - Use sequential build numbers
5. **Regular monitoring** - Check Contrast UI after each deployment
6. **Security controls** - Integrate custom validators/sanitizers
7. **Close sessions** - Always close agent session for proper processing to work with SBAV auto-remediation

## References

- [Contrast Documentation](https://docs.contrastsecurity.com/)
- [Harness Pipeline Guide](./harness-pipeline.md) - Authentication setup
- [Security Controls Guide](./security-controls.md) - Custom controls integration
- [Testing Guide](./testing.md) - Vulnerability test suite

