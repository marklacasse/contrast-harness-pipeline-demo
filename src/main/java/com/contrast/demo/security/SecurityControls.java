package com.contrast.demo.security;

import java.util.regex.Pattern;

/**
 * Custom Security Controls for Contrast IAST Agent
 * 
 * These methods serve as validators and sanitizers that can be registered
 * with Contrast to teach the agent about your application's security controls.
 * 
 * VALIDATORS: Methods that return boolean to indicate if input is safe
 * SANITIZERS: Methods that transform input to make it safe
 * 
 * To register these in Contrast UI:
 * - Go to Application Settings > Security Controls
 * - Add validator/sanitizer with full method signature
 * - Use * marker for tracked parameters/return values
 * 
 * Example signatures for Contrast:
 * - com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
 * - com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
 */
public class SecurityControls {

    // ========== SQL INJECTION VALIDATORS ==========
    
    /**
     * Validator: Check if input is safe for SQL queries
     * Contrast signature: com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
     * 
     * @param input The string to validate
     * @return true if input contains only safe characters for SQL
     */
    public static boolean isSafeSqlInput(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        
        // Allow only alphanumeric, underscore, hyphen, dot, and @ symbol
        // This prevents SQL metacharacters like ', ", ;, --, /*, */, etc.
        Pattern safePattern = Pattern.compile("^[a-zA-Z0-9_@.\\-]+$");
        return safePattern.matcher(input).matches();
    }
    
    /**
     * Validator: Check if username is safe (stricter validation)
     * Contrast signature: com.contrast.demo.security.SecurityControls.isSafeUsername(java.lang.String*)
     * 
     * @param username The username to validate
     * @return true if username contains only safe characters
     */
    public static boolean isSafeUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        // Username: 3-20 chars, alphanumeric and underscore only
        Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
        return usernamePattern.matcher(username).matches();
    }
    
    /**
     * Validator: Check if input is numeric only
     * Contrast signature: com.contrast.demo.security.SecurityControls.isNumeric(java.lang.String*)
     * 
     * @param input The string to validate
     * @return true if input is a valid number
     */
    public static boolean isNumeric(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        try {
            Long.parseLong(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // ========== SQL INJECTION SANITIZERS ==========
    
    /**
     * Sanitizer: Escape SQL special characters
     * Contrast signature: com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
     * 
     * @param input The string to sanitize
     * @return Sanitized string safe for SQL queries
     */
    public static String sanitizeSqlInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Escape SQL metacharacters
        return input
            .replace("'", "''")           // Single quote
            .replace("\"", "\"\"")        // Double quote
            .replace("\\", "\\\\")        // Backslash
            .replace(";", "")             // Remove semicolon
            .replace("--", "")            // Remove SQL comment
            .replace("/*", "")            // Remove multi-line comment start
            .replace("*/", "")            // Remove multi-line comment end
            .replace("xp_", "")           // Remove extended stored procedures
            .replace("sp_", "");          // Remove stored procedures
    }
    
    // ========== XSS VALIDATORS ==========
    
    /**
     * Validator: Check if input is safe for HTML output
     * Contrast signature: com.contrast.demo.security.SecurityControls.isSafeHtmlInput(java.lang.String*)
     * 
     * @param input The string to validate
     * @return true if input doesn't contain HTML/JavaScript characters
     */
    public static boolean isSafeHtmlInput(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        
        // Check for dangerous characters and patterns
        String lowerInput = input.toLowerCase();
        
        // Reject if contains HTML tags or JavaScript patterns
        if (lowerInput.contains("<script") || 
            lowerInput.contains("</script") ||
            lowerInput.contains("javascript:") ||
            lowerInput.contains("onerror=") ||
            lowerInput.contains("onload=") ||
            input.contains("<") ||
            input.contains(">")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validator: Check if input matches safe text pattern (no HTML/JS)
     * Contrast signature: com.contrast.demo.security.SecurityControls.isSafeTextPattern(java.lang.String*)
     * 
     * @param input The string to validate
     * @return true if input is safe text
     */
    public static boolean isSafeTextPattern(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        
        // Allow letters, numbers, spaces, and basic punctuation
        Pattern safePattern = Pattern.compile("^[a-zA-Z0-9 .,!?\\-@]+$");
        return safePattern.matcher(input).matches();
    }
    
    // ========== XSS SANITIZERS ==========
    
    /**
     * Sanitizer: HTML encode special characters
     * Contrast signature: com.contrast.demo.security.SecurityControls.sanitizeHtmlOutput(java.lang.String*)
     * 
     * @param input The string to sanitize
     * @return HTML-encoded string safe for output
     */
    public static String sanitizeHtmlOutput(String input) {
        if (input == null) {
            return null;
        }
        
        // HTML encode dangerous characters
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }
    
    /**
     * Sanitizer: Remove HTML tags completely
     * Contrast signature: com.contrast.demo.security.SecurityControls.stripHtmlTags(java.lang.String*)
     * 
     * @param input The string to sanitize
     * @return String with all HTML tags removed
     */
    public static String stripHtmlTags(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove all HTML tags
        return input.replaceAll("<[^>]*>", "");
    }
    
    // ========== COMMAND INJECTION VALIDATORS ==========
    
    /**
     * Validator: Check if input is safe for command execution
     * Contrast signature: com.contrast.demo.security.SecurityControls.isSafeCommandInput(java.lang.String*)
     * 
     * @param input The string to validate
     * @return true if input doesn't contain shell metacharacters
     */
    public static boolean isSafeCommandInput(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        
        // Reject shell metacharacters
        String[] dangerousChars = {";", "|", "&", "$", "`", "\n", "(", ")", "<", ">", "\\", "!"};
        for (String dangerousChar : dangerousChars) {
            if (input.contains(dangerousChar)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validator: Check if input is a valid hostname/IP
     * Contrast signature: com.contrast.demo.security.SecurityControls.isValidHost(java.lang.String*)
     * 
     * @param host The hostname to validate
     * @return true if hostname is valid
     */
    public static boolean isValidHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        
        // Valid hostname or IP address pattern
        Pattern hostPattern = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9.-]{0,61}[a-zA-Z0-9]$|^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
        return hostPattern.matcher(host).matches();
    }
    
    // ========== COMMAND INJECTION SANITIZERS ==========
    
    /**
     * Sanitizer: Remove shell metacharacters from input
     * Contrast signature: com.contrast.demo.security.SecurityControls.sanitizeCommandInput(java.lang.String*)
     * 
     * @param input The string to sanitize
     * @return Sanitized string safe for command execution
     */
    public static String sanitizeCommandInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove all shell metacharacters
        return input
            .replaceAll("[;|&$`\\n()<>\\\\!]", "")
            .trim();
    }
    
    // ========== PATH TRAVERSAL VALIDATORS ==========
    
    /**
     * Validator: Check if path is safe (no directory traversal)
     * Contrast signature: com.contrast.demo.security.SecurityControls.isSafePath(java.lang.String*)
     * 
     * @param path The path to validate
     * @return true if path doesn't contain traversal sequences
     */
    public static boolean isSafePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // Reject path traversal sequences
        return !path.contains("..") && 
               !path.contains("./") && 
               !path.contains("~") &&
               !path.startsWith("/");
    }
    
    /**
     * Sanitizer: Remove path traversal sequences
     * Contrast signature: com.contrast.demo.security.SecurityControls.sanitizePath(java.lang.String*)
     * 
     * @param path The path to sanitize
     * @return Sanitized path
     */
    public static String sanitizePath(String path) {
        if (path == null) {
            return null;
        }
        
        // Remove dangerous path elements
        return path
            .replace("..", "")
            .replace("./", "")
            .replace("~", "")
            .replaceAll("^/+", "");
    }
    
    // ========== LDAP INJECTION VALIDATORS ==========
    
    /**
     * Validator: Check if input is safe for LDAP queries
     * Contrast signature: com.contrast.demo.security.SecurityControls.isSafeLdapInput(java.lang.String*)
     * 
     * @param input The string to validate
     * @return true if input doesn't contain LDAP metacharacters
     */
    public static boolean isSafeLdapInput(String input) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        
        // Reject LDAP metacharacters
        String[] ldapMetachars = {"*", "(", ")", "\\", "/", "\0"};
        for (String metachar : ldapMetachars) {
            if (input.contains(metachar)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Sanitizer: Escape LDAP special characters
     * Contrast signature: com.contrast.demo.security.SecurityControls.sanitizeLdapInput(java.lang.String*)
     * 
     * @param input The string to sanitize
     * @return Sanitized string safe for LDAP queries
     */
    public static String sanitizeLdapInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Escape LDAP metacharacters
        return input
            .replace("\\", "\\5c")
            .replace("*", "\\2a")
            .replace("(", "\\28")
            .replace(")", "\\29")
            .replace("\0", "\\00");
    }
    
    // ========== EMAIL VALIDATORS ==========
    
    /**
     * Validator: Check if input is a valid email address
     * Contrast signature: com.contrast.demo.security.SecurityControls.isValidEmail(java.lang.String*)
     * 
     * @param email The email to validate
     * @return true if email is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        // Basic email pattern validation
        Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        return emailPattern.matcher(email).matches();
    }
    
    // ========== URL VALIDATORS ==========
    
    /**
     * Validator: Check if URL is safe (whitelist approach)
     * Contrast signature: com.contrast.demo.security.SecurityControls.isSafeUrl(java.lang.String*)
     * 
     * @param url The URL to validate
     * @return true if URL uses safe protocol
     */
    public static boolean isSafeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // Only allow http and https protocols
        String lowerUrl = url.toLowerCase();
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://");
    }
}
