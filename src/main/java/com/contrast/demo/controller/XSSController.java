package com.contrast.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * OWASP A03:2021 - Cross-Site Scripting (XSS)
 * This controller demonstrates XSS vulnerabilities
 */
@Controller
@RequestMapping("/xss")
public class XSSController {

    @GetMapping
    public String xssHome(Model model) {
        return "xss";
    }

    /**
     * Reflected XSS
     * User input reflected directly in response
     */
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String query, Model model) {
        // VULNERABLE: No sanitization of user input
        if (query != null) {
            model.addAttribute("query", query);
            model.addAttribute("unsafeQuery", query); // Will be rendered unescaped
        }
        return "xss-search";
    }

    /**
     * DOM-based XSS
     * JavaScript processes user input
     */
    @GetMapping("/greeting")
    public String greeting(@RequestParam(required = false) String name, Model model) {
        // VULNERABLE: Name will be used in JavaScript without sanitization
        model.addAttribute("name", name != null ? name : "Guest");
        return "xss-greeting";
    }

    /**
     * Stored XSS simulation
     * Stores and displays user input without sanitization
     */
    @PostMapping("/comment")
    @ResponseBody
    public String postComment(@RequestParam String comment) {
        // VULNERABLE: Comment stored and displayed without encoding
        return "<html><body>" +
               "<h2>Comment Posted:</h2>" +
               "<div>" + comment + "</div>" +
               "<p>Try: &lt;script&gt;alert('XSS')&lt;/script&gt;</p>" +
               "</body></html>";
    }

    /**
     * HTML Injection
     * Allows arbitrary HTML in user profile
     */
    @PostMapping("/update-bio")
    @ResponseBody
    public String updateBio(@RequestParam String bio) {
        // VULNERABLE: HTML not sanitized
        return "<html><body>" +
               "<h2>Profile Updated:</h2>" +
               "<div class='bio'>" + bio + "</div>" +
               "</body></html>";
    }
}
