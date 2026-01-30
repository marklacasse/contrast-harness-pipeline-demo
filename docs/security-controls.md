# Security Controls Guide

## Overview

Custom security controls teach the Contrast IAST agent about your application's security measures. When the agent observes these controls being called in data flow, it can automatically remediate or suppress vulnerabilities.

## What Are Security Controls?

### Validators
- **Purpose**: Boolean methods that check if input is safe
- **Return**: `true` if safe, `false` if dangerous
- **Example**: `isSafeSqlInput(String input)` returns boolean
- **Contrast behavior**: Marks tracked strings as validated

### Sanitizers
- **Purpose**: Methods that transform input to make it safe
- **Return**: New safe string
- **Example**: `sanitizeSqlInput(String input)` returns sanitized String
- **Contrast behavior**: Applies "sanitized" tags to returned string

## SecurityControls Library

**Location:** `src/main/java/com/contrast/demo/security/SecurityControls.java`

### SQL Injection Controls

**Validators:**
```java
// Allows alphanumeric, underscore, hyphen, dot, @
SecurityControls.isSafeSqlInput(String input)

// Username: 3-20 chars, alphanumeric and underscore only
SecurityControls.isSafeUsername(String input)

// Numeric validation
SecurityControls.isNumeric(String input)
```

**Sanitizers:**
```java
// Escapes SQL metacharacters (', ", \, ;, --, /*, */)
SecurityControls.sanitizeSqlInput(String input)
```

### XSS/HTML Injection Controls

**Validators:**
```java
// Checks for HTML/JavaScript characters
SecurityControls.isSafeHtmlInput(String input)

// Validates against pattern (alphanumeric, space, common punctuation)
SecurityControls.isSafeTextPattern(String input)
```

**Sanitizers:**
```java
// Escapes HTML entities (&, <, >, ", ')
SecurityControls.sanitizeHtmlOutput(String input)

// Alternative escaping
SecurityControls.escapeHtml(String input)
```

### Command Injection Controls

**Validators:**
```java
// Validates command arguments (alphanumeric, ., -, _)
SecurityControls.isSafeCommandInput(String input)

// Validates filename (alphanumeric, ., -, _)
SecurityControls.isSafeFilename(String input)
```

**Sanitizers:**
```java
// Removes shell metacharacters
SecurityControls.sanitizeCommandInput(String input)
```

### Path Traversal Controls

**Validators:**
```java
// Validates path (no .., no absolute paths)
SecurityControls.isSafePath(String input)
```

**Sanitizers:**
```java
// Normalizes and validates path
SecurityControls.sanitizePath(String input)
```

### LDAP Injection Controls

**Validators:**
```java
// Validates LDAP filter input
SecurityControls.isSafeLdapInput(String input)
```

**Sanitizers:**
```java
// Escapes LDAP special characters
SecurityControls.sanitizeLdapInput(String input)
```

### Additional Validators

```java
// RFC 5322 compliant email validation
SecurityControls.isValidEmail(String email)

// URL validation
SecurityControls.isValidUrl(String url)
```

## Using Security Controls

### In Your Code

**Example 1: Validator (checking)**
```java
@PostMapping("/sql")
public String sqlEndpoint(@RequestParam String username) {
    // Validate input - Contrast sees this in data flow
    if (!SecurityControls.isSafeSqlInput(username)) {
        return "Invalid input";
    }
    
    String query = "SELECT * FROM users WHERE username = '" + username + "'";
    // Execute query...
}
```

**Example 2: Sanitizer (transforming)**
```java
@PostMapping("/sql-safe")
public String sqlSafeEndpoint(@RequestParam String username) {
    // Sanitize input - Contrast sees the sanitized string
    String safeUsername = SecurityControls.sanitizeSqlInput(username);
    
    String query = "SELECT * FROM users WHERE username = '" + safeUsername + "'";
    // Execute query...
}
```

**Example 3: Combined (validation + sanitization)**
```java
@PostMapping("/secure")
public String secureEndpoint(@RequestParam String input) {
    // Validate first
    if (!SecurityControls.isSafeSqlInput(input)) {
        // Sanitize if validation fails
        input = SecurityControls.sanitizeSqlInput(input);
    }
    
    String query = "SELECT * FROM data WHERE field = '" + input + "'";
    // Execute query...
}
```

## Registering Controls in Contrast UI

After deploying code with security controls:

1. **Observe Controls in Stack Traces**
   - Deploy application with Contrast agent
   - Trigger vulnerabilities
   - Check stack traces for SecurityControls methods

2. **Register in Contrast UI** (Optional)
   - Navigate to: Policy → Security Controls
   - Click: Add Security Control
   - Select: Java
   - Input method signature with wildcards

### Method Signatures for Contrast UI

Use these signatures to register controls:

**SQL Injection:**
```
com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
```

**Command Injection:**
```
com.contrast.demo.security.SecurityControls.isSafeCommandInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizeCommandInput(java.lang.String*)
```

**LDAP Injection:**
```
com.contrast.demo.security.SecurityControls.isSafeLdapInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizeLdapInput(java.lang.String*)
```

**XSS:**
```
com.contrast.demo.security.SecurityControls.isSafeHtmlInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizeHtmlOutput(java.lang.String*)
```

**Path Traversal:**
```
com.contrast.demo.security.SecurityControls.isSafePath(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizePath(java.lang.String*)
```

## How Auto-Remediation Works

1. **Integration**: Call SecurityControls methods before vulnerable operations
2. **Observation**: Contrast agent sees methods in data flow during runtime
3. **Session Close**: Pipeline closes agent session, triggering analysis
4. **Auto-Remediation**: Contrast recognizes controls and remediates vulnerabilities

Example from pipeline:
```bash
# After tests, close agent session
curl -X POST "${API_HOST}/Contrast/api/ng/organizations/${ORG_ID}/agent-sessions/sbav" \
  -d '{"appName":"app","metadata":[{"label":"buildNumber","value":"123"}]}'

# Contrast processes observations and auto-remediates
```

## Best Practices

1. **Call controls early**: Place validation/sanitization before vulnerable operations
2. **Be consistent**: Use same controls across similar operations
3. **Log rejections**: Log when validation fails for debugging
4. **Don't bypass**: Ensure all code paths call security controls
5. **Test thoroughly**: Verify controls work as expected in unit tests

## Testing Security Controls

The application includes test suite for triggering vulnerabilities:
```bash
./tests/run-route-tests.sh http://localhost:8080
```

After running tests:
- Check Contrast UI for detected vulnerabilities
- Verify SecurityControls appear in stack traces
- Confirm auto-remediation for protected endpoints
