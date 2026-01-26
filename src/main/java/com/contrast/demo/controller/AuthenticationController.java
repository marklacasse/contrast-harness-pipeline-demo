package com.contrast.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * OWASP A07:2021 - Identification and Authentication Failures
 * This controller demonstrates authentication vulnerabilities
 */
@Controller
@RequestMapping("/auth")
public class AuthenticationController {

    @GetMapping
    public String authHome(Model model) {
        return "authentication";
    }

    /**
     * No Rate Limiting
     * Allows unlimited login attempts (brute force vulnerability)
     */
    @PostMapping("/login")
    @ResponseBody
    public String login(@RequestParam String username, @RequestParam String password) {
        // VULNERABLE: No rate limiting, no account lockout
        if ("admin".equals(username) && "admin123".equals(password)) {
            return "Login successful!";
        }
        return "Login failed! (No rate limiting - try brute force)";
    }

    /**
     * Weak Password Policy
     * Accepts any password without validation
     */
    @PostMapping("/register")
    @ResponseBody
    public String register(@RequestParam String username, 
                          @RequestParam String password,
                          @RequestParam String email) {
        // VULNERABLE: No password strength requirements
        return "User registered successfully!\n" +
               "Username: " + username + "\n" +
               "Password: " + password + " (stored in plain text!)\n" +
               "Email: " + email;
    }

    /**
     * Session Fixation
     * Doesn't regenerate session ID after login
     */
    @PostMapping("/session-login")
    @ResponseBody
    public String sessionLogin(@RequestParam String username, 
                               @RequestParam String password,
                               jakarta.servlet.http.HttpSession session) {
        // VULNERABLE: Session ID not regenerated after authentication
        if ("admin".equals(username) && "admin123".equals(password)) {
            session.setAttribute("user", username);
            session.setAttribute("role", "admin");
            return "Logged in! Session ID: " + session.getId() +
                   "\n(Session ID should be regenerated after login)";
        }
        return "Login failed!";
    }

    /**
     * Predictable Password Reset Token
     * Uses easily guessable reset tokens
     */
    @PostMapping("/forgot-password")
    @ResponseBody
    public String forgotPassword(@RequestParam String email) {
        // VULNERABLE: Predictable reset token
        String resetToken = String.valueOf(System.currentTimeMillis());
        return "Password reset link sent to: " + email +
               "\nReset token: " + resetToken +
               "\n(Token is predictable and based on timestamp!)";
    }

    /**
     * Username Enumeration
     * Different responses reveal if username exists
     */
    @PostMapping("/check-username")
    @ResponseBody
    public String checkUsername(@RequestParam String username) {
        // VULNERABLE: Reveals if username exists
        if ("admin".equals(username) || "user".equals(username)) {
            return "Username already exists!";
        }
        return "Username available!";
    }
}
