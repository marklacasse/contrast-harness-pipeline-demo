package com.contrast.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * OWASP A09:2021 - Security Logging and Monitoring Failures
 * This controller demonstrates logging vulnerabilities
 */
@Controller
@RequestMapping("/logging")
public class LoggingController {

    @GetMapping
    public String loggingHome(Model model) {
        return "logging";
    }

    /**
     * Log Injection
     * User input injected into logs without sanitization
     */
    @PostMapping("/login-attempt")
    @ResponseBody
    public String logLoginAttempt(@RequestParam String username, 
                                  @RequestParam String password) {
        // VULNERABLE: Log injection - user can inject fake log entries
        String logEntry = "Login attempt - Username: " + username + 
                         ", Password: " + password;
        System.out.println(logEntry);
        
        return "Login attempt logged (INSECURE - passwords in logs!):\n" + logEntry +
               "\n\nTry username: admin\\nSUCCESS: Admin logged in";
    }

    /**
     * Insufficient Logging
     * Critical actions not logged
     */
    @PostMapping("/delete-account")
    @ResponseBody
    public String deleteAccount(@RequestParam String userId) {
        // VULNERABLE: Critical action not logged
        return "Account " + userId + " deleted!\n" +
               "No audit trail created!";
    }

    /**
     * Sensitive Data in Logs
     * Logs contain sensitive information
     */
    @PostMapping("/process-payment")
    @ResponseBody
    public String processPayment(@RequestParam String cardNumber,
                                 @RequestParam String cvv,
                                 @RequestParam String amount) {
        // VULNERABLE: Sensitive data logged
        String logMessage = "Payment processed - Card: " + cardNumber + 
                           ", CVV: " + cvv + ", Amount: " + amount;
        System.out.println(logMessage);
        
        return "Payment processed!\n" +
               "Logged (INSECURE): " + logMessage;
    }

    /**
     * No Failed Login Monitoring
     * Failed attempts not tracked or alerted
     */
    @PostMapping("/check-login")
    @ResponseBody
    public String checkLogin(@RequestParam String username) {
        // VULNERABLE: No monitoring of failed attempts
        return "Login check for: " + username + 
               "\nFailed attempts not monitored!";
    }
}
