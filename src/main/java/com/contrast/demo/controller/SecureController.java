package com.contrast.demo.controller;

import com.contrast.demo.model.User;
import com.contrast.demo.repository.UserRepository;
import com.contrast.demo.security.SecurityControls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Secure Implementation Examples
 * 
 * This controller demonstrates how to use SecurityControls validators and sanitizers
 * to remediate vulnerabilities while teaching Contrast about your security measures.
 * 
 * These methods can be used as examples for updating InjectionController and XSSController
 * once the security controls are registered in Contrast.
 */
@Controller
@RequestMapping("/secure")
public class SecureController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public String secureHome(Model model) {
        return "secure";
    }

    // ========== SQL INJECTION - Validator Approach ==========
    
    /**
     * SQL Query with Validation
     * 
     * Uses validator to check if input is safe before query.
     * Once registered in Contrast, this will suppress SQLi findings.
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.isSafeSqlInput(java.lang.String*)
     */
    @PostMapping("/sql-validated")
    @ResponseBody
    public String sqlWithValidation(@RequestParam String username, @RequestParam String password) {
        // Validate inputs before using them
        if (!SecurityControls.isSafeSqlInput(username) || 
            !SecurityControls.isSafeSqlInput(password)) {
            return "Error: Invalid input detected. Only alphanumeric characters allowed.";
        }
        
        try {
            // After validation, Contrast knows this is safe
            String query = "SELECT * FROM users WHERE username = '" + username + 
                          "' AND password = '" + password + "'";
            
            List<User> users = jdbcTemplate.query(query, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                return user;
            });
            
            if (!users.isEmpty()) {
                return "Login successful! Welcome " + users.get(0).getUsername();
            } else {
                return "Login failed!";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // ========== SQL INJECTION - Sanitizer Approach ==========
    
    /**
     * SQL Query with Sanitization
     * 
     * Uses sanitizer to clean input before query.
     * Once registered in Contrast, this will suppress SQLi findings.
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.sanitizeSqlInput(java.lang.String*)
     */
    @PostMapping("/sql-sanitized")
    @ResponseBody
    public String sqlWithSanitization(@RequestParam String username, @RequestParam String password) {
        try {
            // Sanitize inputs - Contrast will track that returned values are safe
            String safeUsername = SecurityControls.sanitizeSqlInput(username);
            String safePassword = SecurityControls.sanitizeSqlInput(password);
            
            // Now using sanitized values - Contrast knows this is safe
            String query = "SELECT * FROM users WHERE username = '" + safeUsername + 
                          "' AND password = '" + safePassword + "'";
            
            List<User> users = jdbcTemplate.query(query, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                return user;
            });
            
            if (!users.isEmpty()) {
                return "Login successful! Welcome " + users.get(0).getUsername();
            } else {
                return "Login failed!";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // ========== SQL INJECTION - Combined Approach (Recommended) ==========
    
    /**
     * SQL Query with Both Validation and Sanitization
     * 
     * Best practice: Validate first (reject bad input), sanitize as backup.
     */
    @PostMapping("/sql-secure")
    @ResponseBody
    public String sqlSecure(@RequestParam String username, @RequestParam String password) {
        // First, try strict validation
        if (SecurityControls.isSafeUsername(username)) {
            // Username is strictly valid, proceed
        } else if (SecurityControls.isSafeSqlInput(username)) {
            // Less strict but still safe
            username = SecurityControls.sanitizeSqlInput(username);
        } else {
            return "Error: Invalid username format";
        }
        
        // Sanitize password as backup
        String safePassword = SecurityControls.sanitizeSqlInput(password);
        
        try {
            String query = "SELECT * FROM users WHERE username = '" + username + 
                          "' AND password = '" + safePassword + "'";
            
            List<User> users = jdbcTemplate.query(query, (rs, rowNum) -> {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                return user;
            });
            
            if (!users.isEmpty()) {
                return "Login successful! Welcome " + users.get(0).getUsername();
            }
            return "Login failed!";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // ========== XSS - Validator Approach ==========
    
    /**
     * HTML Output with Validation
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.isSafeHtmlInput(java.lang.String*)
     */
    @PostMapping("/xss-validated")
    @ResponseBody
    public String xssWithValidation(@RequestParam String comment) {
        // Validate input doesn't contain HTML/JavaScript
        if (!SecurityControls.isSafeHtmlInput(comment)) {
            return "Error: HTML tags and scripts are not allowed";
        }
        
        // After validation, Contrast knows this is safe
        return "<html><body>" +
               "<h2>Comment Posted:</h2>" +
               "<div>" + comment + "</div>" +
               "</body></html>";
    }
    
    // ========== XSS - Sanitizer Approach ==========
    
    /**
     * HTML Output with Sanitization (HTML Encoding)
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.sanitizeHtmlOutput(java.lang.String*)
     */
    @PostMapping("/xss-sanitized")
    @ResponseBody
    public String xssWithSanitization(@RequestParam String comment) {
        // Sanitize by HTML encoding - Contrast tracks the safe return value
        String safeComment = SecurityControls.sanitizeHtmlOutput(comment);
        
        // Now using sanitized value - Contrast knows this is safe
        return "<html><body>" +
               "<h2>Comment Posted:</h2>" +
               "<div>" + safeComment + "</div>" +
               "</body></html>";
    }
    
    /**
     * HTML Output with Tag Stripping
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.stripHtmlTags(java.lang.String*)
     */
    @PostMapping("/xss-stripped")
    @ResponseBody
    public String xssWithStripping(@RequestParam String comment) {
        // Remove all HTML tags
        String strippedComment = SecurityControls.stripHtmlTags(comment);
        
        return "<html><body>" +
               "<h2>Comment Posted:</h2>" +
               "<div>" + strippedComment + "</div>" +
               "</body></html>";
    }
    
    // ========== Command Injection - Validator Approach ==========
    
    /**
     * Command Execution with Validation
     * 
     * Contrast Security Controls:
     * com.contrast.demo.security.SecurityControls.isValidHost(java.lang.String*)
     * com.contrast.demo.security.SecurityControls.isSafeCommandInput(java.lang.String*)
     */
    @PostMapping("/command-validated")
    @ResponseBody
    public String commandWithValidation(@RequestParam String host) {
        // Validate hostname format
        if (!SecurityControls.isValidHost(host)) {
            return "Error: Invalid hostname format";
        }
        
        // Additional check for shell metacharacters
        if (!SecurityControls.isSafeCommandInput(host)) {
            return "Error: Dangerous characters detected";
        }
        
        try {
            // After validation, Contrast knows this is safe
            String command = "ping -c 3 " + host;
            Process process = Runtime.getRuntime().exec(command);
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            return "Command output:\n" + output.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // ========== LDAP Injection - Validator and Sanitizer ==========
    
    /**
     * LDAP Query with Validation
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.isSafeLdapInput(java.lang.String*)
     */
    @PostMapping("/ldap-validated")
    @ResponseBody
    public String ldapWithValidation(@RequestParam String username) {
        // Validate LDAP input
        if (!SecurityControls.isSafeLdapInput(username)) {
            return "Error: Invalid characters for LDAP query";
        }
        
        // After validation, Contrast knows this is safe
        String filter = "(&(uid=" + username + ")(objectClass=person))";
        return "LDAP Filter (validated): " + filter;
    }
    
    /**
     * LDAP Query with Sanitization
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.sanitizeLdapInput(java.lang.String*)
     */
    @PostMapping("/ldap-sanitized")
    @ResponseBody
    public String ldapWithSanitization(@RequestParam String username) {
        // Sanitize LDAP input
        String safeUsername = SecurityControls.sanitizeLdapInput(username);
        
        // Using sanitized value - Contrast knows this is safe
        String filter = "(&(uid=" + safeUsername + ")(objectClass=person))";
        return "LDAP Filter (sanitized): " + filter;
    }
    
    // ========== Email Validation Example ==========
    
    /**
     * Email Processing with Validation
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.isValidEmail(java.lang.String*)
     */
    @PostMapping("/email-validated")
    @ResponseBody
    public String emailWithValidation(@RequestParam String email) {
        if (!SecurityControls.isValidEmail(email)) {
            return "Error: Invalid email format";
        }
        
        return "Email validated successfully: " + email;
    }
    
    // ========== URL Validation Example ==========
    
    /**
     * URL Processing with Validation
     * 
     * Contrast Security Control:
     * com.contrast.demo.security.SecurityControls.isSafeUrl(java.lang.String*)
     */
    @PostMapping("/url-validated")
    @ResponseBody
    public String urlWithValidation(@RequestParam String url) {
        if (!SecurityControls.isSafeUrl(url)) {
            return "Error: Only http:// and https:// URLs are allowed";
        }
        
        return "URL validated successfully: " + url;
    }
}
