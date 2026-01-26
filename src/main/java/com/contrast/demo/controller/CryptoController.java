package com.contrast.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * OWASP A02:2021 - Cryptographic Failures
 * This controller demonstrates cryptographic vulnerabilities
 */
@Controller
@RequestMapping("/crypto")
public class CryptoController {

    @GetMapping
    public String cryptoHome(Model model) {
        return "crypto";
    }

    /**
     * Weak Hashing Algorithm
     * Uses MD5 for password hashing (insecure)
     */
    @PostMapping("/hash-password")
    @ResponseBody
    public String hashPassword(@RequestParam String password) {
        try {
            // VULNERABLE: MD5 is cryptographically broken
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
            String hashedPassword = Base64.getEncoder().encodeToString(hash);
            
            return "Password hashed with MD5 (INSECURE):\n" + hashedPassword;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Hardcoded Credentials
     * Contains hardcoded encryption keys
     */
    @GetMapping("/get-api-key")
    @ResponseBody
    public String getApiKey() {
        // VULNERABLE: Hardcoded API key
        String apiKey = "sk-1234567890abcdef1234567890abcdef";
        String dbPassword = "SuperSecret123!";
        
        return "API Key: " + apiKey + "\nDB Password: " + dbPassword +
               "\n\nThese should never be hardcoded!";
    }

    /**
     * Sensitive Data Exposure
     * Returns sensitive information without encryption
     */
    @GetMapping("/user-details")
    @ResponseBody
    public String getUserDetails(@RequestParam String userId) {
        // VULNERABLE: Sending sensitive data in plain text
        return "{\n" +
               "  \"userId\": \"" + userId + "\",\n" +
               "  \"ssn\": \"123-45-6789\",\n" +
               "  \"creditCard\": \"4532-1234-5678-9010\",\n" +
               "  \"password\": \"plainTextPassword123\"\n" +
               "}";
    }

    /**
     * Insecure Random Number Generation
     * Uses Math.random() for security-sensitive operations
     */
    @GetMapping("/generate-token")
    @ResponseBody
    public String generateToken() {
        // VULNERABLE: Math.random() is not cryptographically secure
        String token = String.valueOf(Math.random()).substring(2);
        return "Generated token (INSECURE): " + token +
               "\n\nUse SecureRandom for security-sensitive tokens!";
    }
}
