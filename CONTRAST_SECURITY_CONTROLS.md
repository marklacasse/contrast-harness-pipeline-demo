# Contrast Security Controls Integration Guide

## Overview

This guide explains how to use custom security controls with Contrast IAST agent to teach it about your application's security measures.

## What are Security Controls?

### Validators
- **Purpose**: Boolean methods that check if input is safe
- **Behavior**: Return `true` if safe, `false` if dangerous
- **How Contrast uses them**: Marks tracked strings as validated (safe)
- **Important**: Does NOT modify the data, only validates it

### Sanitizers
- **Purpose**: Methods that transform input to make it safe
- **Behavior**: Return a new safe string
- **How Contrast uses them**: Applies "sanitized" tags to returned string
- **Important**: Actively changes the data to ensure safety

### Regex Validators (Java only)
- **Purpose**: Compare regex pattern to validate input
- **Behavior**: Uses Pattern.match() with specified regex
- **How Contrast uses them**: Instruments Java regex APIs to detect validation

## Security Controls in This Application

All security controls are in: `com.contrast.demo.security.SecurityControls`

### SQL Injection Controls

#### Validators
```java
// Signature: com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
isSafeSqlInput(String input)
- Checks for SQL metacharacters (', ", ;, --, /*, etc.)
- Returns true if only alphanumeric, underscore, hyphen, dot, @

// Signature: com.contrast.demo.security.SecurityControls.isSafeUsername(java.lang.String*)
isSafeUsername(String username)
- Validates username format (3-20 chars, alphanumeric + underscore)
- Stricter validation for usernames

// Signature: com.contrast.demo.security.SecurityControls.isNumeric(java.lang.String*)
isNumeric(String input)
- Validates input is a valid number
- Useful for ID parameters
```

#### Sanitizers
```java
// Signature: com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
sanitizeSqlInput(String input)
- Escapes SQL special characters
- Removes dangerous patterns (;, --, /*, sp_, xp_)
```

### XSS (Cross-Site Scripting) Controls

#### Validators
```java
// Signature: com.contrast.demo.security.SecurityControls.isSafeHtmlInput(java.lang.String*)
isSafeHtmlInput(String input)
- Checks for HTML tags and JavaScript patterns
- Rejects <script>, javascript:, event handlers

// Signature: com.contrast.demo.security.SecurityControls.isSafeTextPattern(java.lang.String*)
isSafeTextPattern(String input)
- Validates input matches safe text pattern
- Allows letters, numbers, spaces, basic punctuation
```

#### Sanitizers
```java
// Signature: com.contrast.demo.security.SecurityControls.sanitizeHtmlOutput(java.lang.String*)
sanitizeHtmlOutput(String input)
- HTML encodes special characters (&lt; &gt; &quot; etc.)

// Signature: com.contrast.demo.security.SecurityControls.stripHtmlTags(java.lang.String*)
stripHtmlTags(String input)
- Completely removes all HTML tags
```

### Command Injection Controls

#### Validators
```java
// Signature: com.contrast.demo.security.SecurityControls.isSafeCommandInput(java.lang.String*)
isSafeCommandInput(String input)
- Checks for shell metacharacters (;, |, &, $, `, etc.)

// Signature: com.contrast.demo.security.SecurityControls.isValidHost(java.lang.String*)
isValidHost(String host)
- Validates hostname or IP address format
```

#### Sanitizers
```java
// Signature: com.contrast.demo.security.SecurityControls.sanitizeCommandInput(java.lang.String*)
sanitizeCommandInput(String input)
- Removes all shell metacharacters
```

### Path Traversal Controls

```java
// Signature: com.contrast.demo.security.SecurityControls.isSafePath(java.lang.String*)
isSafePath(String path)
- Checks for directory traversal sequences (.., ./, ~)

// Signature: com.contrast.demo.security.SecurityControls.sanitizePath(java.lang.String*)
sanitizePath(String path)
- Removes path traversal sequences
```

### LDAP Injection Controls

```java
// Signature: com.contrast.demo.security.SecurityControls.isSafeLdapInput(java.lang.String*)
isSafeLdapInput(String input)
- Checks for LDAP metacharacters (*, (, ), \, /)

// Signature: com.contrast.demo.security.SecurityControls.sanitizeLdapInput(java.lang.String*)
sanitizeLdapInput(String input)
- Escapes LDAP special characters
```

### General Validators

```java
// Signature: com.contrast.demo.security.SecurityControls.isValidEmail(java.lang.String*)
isValidEmail(String email)
- Validates email address format

// Signature: com.contrast.demo.security.SecurityControls.isSafeUrl(java.lang.String*)
isSafeUrl(String url)
- Validates URL uses safe protocol (http/https only)
```

## How to Register Security Controls in Contrast

### Step 1: Navigate to Security Controls
1. Log into Contrast TeamServer
2. Select your application: **contrast-harness-demo**
3. Go to **Application Settings** → **Security Controls**

### Step 2: Add a Validator
1. Click **"Add Security Control"**
2. Select **"Input Validator"**
3. Enter the method signature with `*` marker:
   ```
   com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
   ```
4. Select which vulnerability types this validator applies to:
   - SQL Injection
5. Click **"Save"**

### Step 3: Add a Sanitizer
1. Click **"Add Security Control"**
2. Select **"Input Sanitizer"**
3. Enter the method signature with `*` marker:
   ```
   com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
   ```
4. Select vulnerability types (SQL Injection)
5. Click **"Save"**

### Step 4: Restart Application
**IMPORTANT**: Agents pull security controls during startup
- Restart your application after adding controls
- Look for confirmation in debug logs:
  ```
  [main ContrastPolicy$a] DEBUG - Adding validator: com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
  ```

## Understanding the `*` Marker

The asterisk (`*`) in method signatures tells Contrast to track data flow:

```java
// Validator signature
com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
                                                                          ^
                                                                          |
                                                Track this parameter's data flow
```

**For Validators**: The `*` marks which parameter to validate
**For Sanitizers**: The `*` marks the parameter AND return value to track

## Usage Examples in Code

### Example 1: Validating SQL Input

**Before (Vulnerable)**:
```java
@PostMapping("/sql")
public String sqlInjection(@RequestParam String username, @RequestParam String password) {
    String query = "SELECT * FROM users WHERE username = '" + username + "'";
    // VULNERABLE: No validation
}
```

**After (With Validator)**:
```java
@PostMapping("/sql")
public String sqlInjection(@RequestParam String username, @RequestParam String password) {
    // Validate inputs
    if (!SecurityControls.isSafeSqlInput(username) || 
        !SecurityControls.isSafeSqlInput(password)) {
        return "Invalid input detected";
    }
    
    String query = "SELECT * FROM users WHERE username = '" + username + "'";
    // Contrast knows this is safe because validation passed
}
```

### Example 2: Sanitizing SQL Input

**Before (Vulnerable)**:
```java
String query = "SELECT * FROM users WHERE username = '" + username + "'";
```

**After (With Sanitizer)**:
```java
String safeUsername = SecurityControls.sanitizeSqlInput(username);
String query = "SELECT * FROM users WHERE username = '" + safeUsername + "'";
// Contrast knows safeUsername is sanitized
```

### Example 3: Validating Command Input

**Before (Vulnerable)**:
```java
@PostMapping("/command")
public String commandInjection(@RequestParam String host) {
    String command = "ping -c 3 " + host;
    Process process = Runtime.getRuntime().exec(command);
}
```

**After (With Validator)**:
```java
@PostMapping("/command")
public String commandInjection(@RequestParam String host) {
    if (!SecurityControls.isValidHost(host)) {
        return "Invalid hostname";
    }
    
    String command = "ping -c 3 " + host;
    Process process = Runtime.getRuntime().exec(command);
    // Contrast knows host is validated
}
```

### Example 4: Sanitizing HTML Output

**Before (Vulnerable)**:
```java
@PostMapping("/comment")
public String postComment(@RequestParam String comment) {
    return "<div>" + comment + "</div>";
}
```

**After (With Sanitizer)**:
```java
@PostMapping("/comment")
public String postComment(@RequestParam String comment) {
    String safeComment = SecurityControls.sanitizeHtmlOutput(comment);
    return "<div>" + safeComment + "</div>";
    // Contrast knows safeComment is sanitized for XSS
}
```

## Recommended Security Controls to Register

### For SQL Injection Vulnerabilities:
```
com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.isSafeUsername(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
```

### For XSS Vulnerabilities:
```
com.contrast.demo.security.SecurityControls.isSafeHtmlInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizeHtmlOutput(java.lang.String*)
com.contrast.demo.security.SecurityControls.stripHtmlTags(java.lang.String*)
```

### For Command Injection:
```
com.contrast.demo.security.SecurityControls.isValidHost(java.lang.String*)
com.contrast.demo.security.SecurityControls.isSafeCommandInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizeCommandInput(java.lang.String*)
```

### For LDAP Injection:
```
com.contrast.demo.security.SecurityControls.isSafeLdapInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.sanitizeLdapInput(java.lang.String*)
```

## Exclusions

If you need to exclude specific methods or URLs from Contrast analysis:

### Code Exclusions
For methods without input parameters that should be ignored:
- Navigate to **Application Settings** → **Exclusions** → **Code**
- Add the complete method signature

### URL Exclusions
For specific endpoints that should be ignored:
- Navigate to **Application Settings** → **Exclusions** → **URL**
- Add URL paths (supports regex and wildcards)

Example:
```
/admin/.*
/internal/health
```

### Input Exclusions
For specific parameters/headers that are safe:
- Navigate to **Application Settings** → **Exclusions** → **Input**
- Specify parameter name, header, query string, etc.

## Verification

### 1. Check Agent Logs
After restarting, verify controls are loaded:
```
[main ContrastPolicy$a] DEBUG - Adding validator: com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
[main ContrastPolicy$a] DEBUG - Adding sanitizer: com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
```

### 2. Test Vulnerabilities
1. Run your test suite: `tests/run-route-tests.sh`
2. Check Contrast for findings
3. Vulnerabilities using security controls should be suppressed

### 3. Verify in Contrast UI
1. Go to **Vulnerabilities** tab
2. Previously flagged SQLi/XSS should show as "Suppressed by Security Control"
3. View details to see which control suppressed it

## Best Practices

1. **Use Validators First**: Reject bad input early
2. **Sanitize as Backup**: For legacy code or when rejection isn't possible
3. **Be Specific**: Create targeted validators for specific use cases
4. **Test Thoroughly**: Ensure controls don't break legitimate use cases
5. **Document Controls**: Keep this guide updated as you add controls
6. **Monitor Logs**: Watch for control loading during startup
7. **Restart After Changes**: Always restart after modifying controls

## Troubleshooting

### Controls Not Working?
- ✅ Verify method signature exactly matches (including package)
- ✅ Ensure `*` marker is present for tracked parameters
- ✅ Restart application after adding controls
- ✅ Check agent debug logs for control loading
- ✅ Verify control applies to correct vulnerability types

### Still Seeing Vulnerabilities?
- Maybe the control isn't being called in the code path
- Add logging to verify control methods are executed
- Check if tracked data flows through the control
- Ensure control is registered for the right vulnerability type

## Next Steps

1. ✅ Build and deploy application with SecurityControls class
2. Register key validators/sanitizers in Contrast UI
3. Restart application
4. Run test suite to trigger vulnerabilities
5. Verify suppressions in Contrast
6. Iterate and add more controls as needed
