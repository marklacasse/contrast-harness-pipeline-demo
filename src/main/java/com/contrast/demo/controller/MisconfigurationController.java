package com.contrast.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;

/**
 * OWASP A05:2021 - Security Misconfiguration
 * This controller demonstrates configuration vulnerabilities
 */
@Controller
@RequestMapping("/config")
public class MisconfigurationController {

    @GetMapping
    public String configHome(Model model) {
        return "misconfiguration";
    }

    /**
     * Verbose Error Messages
     * Exposes stack traces and system information
     */
    @GetMapping("/error-test")
    @ResponseBody
    public String errorTest(@RequestParam String input) {
        try {
            // VULNERABLE: Will throw detailed exception
            int result = Integer.parseInt(input);
            return "Parsed: " + result;
        } catch (Exception e) {
            // Exposing full stack trace
            StringWriter sw = new StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return "ERROR (with full stack trace):\n" + sw.toString();
        }
    }

    /**
     * Directory Listing Enabled
     * Shows server directory structure
     */
    @GetMapping("/list-files")
    @ResponseBody
    public String listFiles(@RequestParam String path) {
        // VULNERABLE: Allows directory traversal and listing
        java.io.File dir = new java.io.File(path);
        StringBuilder result = new StringBuilder("Files in " + path + ":\n\n");
        
        if (dir.exists() && dir.isDirectory()) {
            for (java.io.File file : dir.listFiles()) {
                result.append(file.getName()).append("\n");
            }
        }
        
        return result.toString();
    }

    /**
     * Default Credentials
     * Uses well-known default credentials
     */
    @GetMapping("/admin-panel")
    @ResponseBody
    public String adminPanel(@RequestParam(required = false) String user,
                            @RequestParam(required = false) String pass) {
        // VULNERABLE: Default credentials
        if ("admin".equals(user) && "admin".equals(pass)) {
            return "Admin access granted!\n" +
                   "Default credentials still active!";
        }
        return "Login required (try admin/admin)";
    }

    /**
     * Debug Mode Enabled
     * Debug endpoints exposed in production
     */
    @GetMapping("/debug")
    @ResponseBody
    public String debugInfo() {
        // VULNERABLE: Exposes system information
        return "DEBUG INFO:\n" +
               "Java Version: " + System.getProperty("java.version") + "\n" +
               "OS: " + System.getProperty("os.name") + "\n" +
               "User: " + System.getProperty("user.name") + "\n" +
               "Home: " + System.getProperty("user.home") + "\n" +
               "Path: " + System.getProperty("java.class.path") + "\n" +
               "\nThis should not be exposed in production!";
    }

    /**
     * Unnecessary Features Enabled
     * HTTP methods that should be disabled
     */
    @RequestMapping(value = "/resource", method = {RequestMethod.TRACE, RequestMethod.OPTIONS})
    @ResponseBody
    public String unsafeMethods() {
        // VULNERABLE: TRACE method can leak cookies
        return "TRACE and OPTIONS methods enabled (security risk!)";
    }
}
