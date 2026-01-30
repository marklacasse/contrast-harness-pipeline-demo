# Quick Reference: Contrast Security Control Signatures

## Copy-Paste Ready Signatures for Contrast UI

### SQL Injection Controls

**Validators:**
```
com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.isSafeUsername(java.lang.String*)
com.contrast.demo.security.SecurityControls.isNumeric(java.lang.String*)
```

**Sanitizers:**
```
com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
```

**Applies to:** SQL Injection, sql-injection

---

### XSS (Cross-Site Scripting) Controls

**Validators:**
```
com.contrast.demo.security.SecurityControls.isSafeHtmlInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.isSafeTextPattern(java.lang.String*)
```

**Sanitizers:**
```
com.contrast.demo.security.SecurityControls.sanitizeHtmlOutput(java.lang.String*)
com.contrast.demo.security.SecurityControls.stripHtmlTags(java.lang.String*)
```

**Applies to:** Cross-Site Scripting (XSS), xss-reflected, xss-stored

---

### Command Injection Controls

**Validators:**
```
com.contrast.demo.security.SecurityControls.isSafeCommandInput(java.lang.String*)
com.contrast.demo.security.SecurityControls.isValidHost(java.lang.String*)
```

**Sanitizers:**
```
com.contrast.demo.security.SecurityControls.sanitizeCommandInput(java.lang.String*)
```

**Applies to:** Command Injection, cmd-injection

---

### Path Traversal Controls

**Validators:**
```
com.contrast.demo.security.SecurityControls.isSafePath(java.lang.String*)
```

**Sanitizers:**
```
com.contrast.demo.security.SecurityControls.sanitizePath(java.lang.String*)
```

**Applies to:** Path Traversal, path-traversal

---

### LDAP Injection Controls

**Validators:**
```
com.contrast.demo.security.SecurityControls.isSafeLdapInput(java.lang.String*)
```

**Sanitizers:**
```
com.contrast.demo.security.SecurityControls.sanitizeLdapInput(java.lang.String*)
```

**Applies to:** LDAP Injection, ldap-injection

---

### Email Validation

**Validators:**
```
com.contrast.demo.security.SecurityControls.isValidEmail(java.lang.String*)
```

**Applies to:** Header Injection, email-injection

---

### URL Validation (SSRF Protection)

**Validators:**
```
com.contrast.demo.security.SecurityControls.isSafeUrl(java.lang.String*)
```

**Applies to:** Server-Side Request Forgery (SSRF), ssrf

---

## Registration Steps

1. **Login to Contrast:** https://teamserver-staging.contsec.com/
2. **Navigate to Application:** contrast-harness-demo
3. **Go to:** Application Settings → Security Controls
4. **Click:** "Add Security Control"
5. **Select Type:** Input Validator or Input Sanitizer
6. **Copy/Paste Signature** from above (include the `*`)
7. **Select Vulnerability Types** it applies to
8. **Save**
9. **Restart Application** to load new controls

## Expected Debug Log Output

After restarting, you should see in application logs:

```
[main ContrastPolicy$a] DEBUG - Adding validator: com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
[main ContrastPolicy$a] DEBUG - Adding validator: com.contrast.demo.security.SecurityControls.isSafeUsername(java.lang.String*)
[main ContrastPolicy$a] DEBUG - Adding sanitizer: com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
[main ContrastPolicy$a] DEBUG - Adding validator: com.contrast.demo.security.SecurityControls.isSafeHtmlInput(java.lang.String*)
[main ContrastPolicy$a] DEBUG - Adding sanitizer: com.contrast.demo.security.SecurityControls.sanitizeHtmlOutput(java.lang.String*)
```

## Priority Order (Start with These)

### Immediate Impact:
1. `isSafeSqlInput` - SQL Injection validator
2. `sanitizeSqlInput` - SQL Injection sanitizer
3. `sanitizeHtmlOutput` - XSS sanitizer
4. `isValidHost` - Command Injection validator

### Next Priority:
5. `isSafeHtmlInput` - XSS validator
6. `isSafeCommandInput` - Command Injection validator
7. `isSafeLdapInput` - LDAP Injection validator

### As Needed:
8. `isSafeUsername` - Username format validator
9. `isNumeric` - Numeric ID validator
10. `isValidEmail` - Email format validator

## Testing After Registration

1. **Build and deploy:** Run your Harness pipeline
2. **Run tests:** Execute `tests/run-route-tests.sh`
3. **Check Contrast:** 
   - View Vulnerabilities tab
   - Previously flagged issues should show "Suppressed by Security Control"
   - Click on suppressed vulnerability to see which control suppressed it

## Troubleshooting

### Controls Not Loading?
- ✅ Check method signature exactly matches (case-sensitive)
- ✅ Verify `*` marker is present
- ✅ Restart application after adding controls
- ✅ Enable debug logging: `-Dcontrast.level=debug`

### Still Seeing Vulnerabilities?
- Maybe the control isn't being called in your code yet
- Update vulnerable controllers to use SecurityControls methods
- See `SecureController.java` for implementation examples

## Next Steps

1. Register controls in Contrast UI (start with priority list above)
2. Restart application (`kubectl rollout restart deployment/vulnerable-app -n apps`)
3. Run test suite to verify
4. Update existing controllers to use SecurityControls
5. Monitor Contrast for suppressed vulnerabilities
