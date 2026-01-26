package com.contrast.demo.controller;

import com.contrast.demo.model.User;
import com.contrast.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * OWASP A01:2021 - Broken Access Control
 * This controller demonstrates various access control vulnerabilities
 */
@Controller
@RequestMapping("/access-control")
public class AccessControlController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String accessControlHome(Model model) {
        return "access-control";
    }

    /**
     * Insecure Direct Object Reference (IDOR)
     * Users can access any user's profile by changing the ID
     */
    @GetMapping("/profile/{userId}")
    public String viewProfile(@PathVariable Long userId, Model model, HttpSession session) {
        // VULNERABLE: No authorization check - any user can view any profile
        User user = userRepository.findById(userId).orElse(null);
        
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("message", "Viewing profile for user ID: " + userId);
        } else {
            model.addAttribute("message", "User not found");
        }
        
        return "profile";
    }

    /**
     * Missing Function Level Access Control
     * Admin functions accessible without proper role check
     */
    @PostMapping("/admin/delete-user/{userId}")
    @ResponseBody
    public String deleteUser(@PathVariable Long userId, HttpSession session) {
        // VULNERABLE: No admin role check
        try {
            userRepository.deleteById(userId);
            return "User " + userId + " deleted successfully!";
        } catch (Exception e) {
            return "Error deleting user: " + e.getMessage();
        }
    }

    /**
     * Horizontal Privilege Escalation
     * Users can modify other users' data
     */
    @PostMapping("/update-email")
    @ResponseBody
    public String updateEmail(@RequestParam Long userId, 
                             @RequestParam String email,
                             HttpSession session) {
        // VULNERABLE: No check if the userId matches the current user
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setEmail(email);
            userRepository.save(user);
            return "Email updated for user " + userId;
        }
        return "User not found";
    }

    /**
     * Path Traversal
     * Allows reading arbitrary files from the system
     */
    @GetMapping("/download")
    @ResponseBody
    public String downloadFile(@RequestParam String filename) {
        // VULNERABLE: No path validation
        try {
            java.io.File file = new java.io.File(filename);
            if (file.exists()) {
                return "File found: " + file.getAbsolutePath() + 
                       "\nSize: " + file.length() + " bytes";
            }
            return "File not found: " + filename;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
