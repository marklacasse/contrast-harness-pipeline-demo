# Route Coverage Test Suite

This directory contains automated tests to exercise all application routes for IAST security testing with Contrast.

## Overview

The test suite exercises all 50 discovered routes across the OWASP Top 10 vulnerability categories:
- ✅ **Injection** (SQL, Command, LDAP)
- ✅ **Broken Access Control** (IDOR, privilege escalation)
- ✅ **Cryptographic Failures** (weak crypto, hardcoded secrets)
- ✅ **Broken Authentication** (weak passwords, session issues)
- ✅ **XSS** (reflected and stored)
- ✅ **Security Misconfiguration** (debug endpoints, unsafe HTTP methods)
- ✅ **SSRF** (URL fetching, XXE)
- ✅ **Software Integrity Failures** (insecure deserialization)
- ✅ **Logging Failures** (insufficient logging, sensitive data in logs)
- ✅ **H2 Console** (exposed database console)

## Files

- `run-route-tests.sh` - Main test script that exercises all routes

## Local Testing

### Prerequisites

1. Build the application:
```bash
mvn clean package
```

2. Start the application with Contrast agent:
```bash
java -javaagent:./.contrast/contrast-agent-6.25.1.jar \
     -Dcontrast.config.path=./.contrast/contrast.yaml \
     -jar target/vulnerable-app-1.0.0.jar
```

3. Wait for the application to start (check logs for "Started VulnerableApplication")

### Running the Tests

Basic usage (summary output):
```bash
./tests/run-route-tests.sh
```

Verbose output (see each test):
```bash
VERBOSE=true ./tests/run-route-tests.sh
```

Custom base URL:
```bash
BASE_URL=http://localhost:9090 ./tests/run-route-tests.sh
```

### Expected Output

```
==========================================
Vulnerable App Test Suite
Target: http://localhost:8080
==========================================
✓ Application is ready!
Starting route coverage tests...

[INFO] Testing: Home & Health Endpoints
[INFO] Testing: Injection Vulnerabilities
[INFO] Testing: Access Control Vulnerabilities
[INFO] Testing: Cryptographic Failures
[INFO] Testing: Authentication Vulnerabilities
[INFO] Testing: Cross-Site Scripting (XSS)
[INFO] Testing: Security Misconfiguration
[INFO] Testing: Server-Side Request Forgery
[INFO] Testing: Software and Data Integrity Failures
[INFO] Testing: Security Logging and Monitoring Failures
[INFO] Testing: H2 Database Console

==========================================
Test Summary
==========================================
Total Tests:  85+
Passed:       85+ ✓
Failed:       0 ✗
Coverage:     100%
==========================================
✓ All tests passed! Routes exercised successfully.
```

## Verifying Contrast Coverage

After running the tests, check your Contrast dashboard:

1. Navigate to your application: **contrast-harness-pipeline-test** (local) or **contrast-harness-demo** (deployed)
2. Go to **Route Coverage** tab
3. You should see ~100% coverage (50/50 routes exercised)
4. Check **Vulnerabilities** tab for IAST findings across all OWASP Top 10 categories

## CI/CD Integration

This test suite can be integrated into your Harness pipeline:

```yaml
# Example Harness step
- step:
    type: Run
    name: Exercise Routes for IAST
    identifier: exercise_routes
    spec:
      shell: Bash
      command: |
        # Wait for app deployment
        kubectl wait --for=condition=available deployment/vulnapp \
          -n apps --timeout=300s
        
        # Get service URL
        SERVICE_URL=$(kubectl get svc vulnapp -n apps \
          -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
        
        # Run tests
        BASE_URL=http://$SERVICE_URL:8080 ./tests/run-route-tests.sh
```

## Troubleshooting

### Application not available
- Ensure the app is running: `curl http://localhost:8080/actuator/health`
- Check if port 8080 is in use: `lsof -i :8080`
- Review application logs for startup errors

### Tests failing
- Check application logs for errors
- Verify Contrast agent is attached (look for "Contrast agent loaded" in logs)
- Ensure H2 database is accessible (in-memory, should auto-create)

### Low route coverage in Contrast
- Wait a few seconds for Contrast to process traffic
- Refresh the Contrast dashboard
- Check Contrast agent logs for communication issues

## Notes

- These tests intentionally trigger security vulnerabilities for IAST analysis
- Do NOT run against production systems
- Some exploit payloads are benign but demonstrate vulnerability patterns
- The H2 console should be disabled in production (currently enabled for demo purposes)
