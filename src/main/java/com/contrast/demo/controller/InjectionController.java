package com.contrast.demo.controller;

import com.contrast.demo.model.User;
import com.contrast.demo.repository.UserRepository;
import com.contrast.demo.security.SecurityControls;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * OWASP A03:2021 - Injection Vulnerabilities
 * This controller demonstrates various injection vulnerabilities
 */
@Controller
@RequestMapping("/injection")
public class InjectionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public String injectionHome(Model model) {
        return "injection";
    }

    /**
     * SQL Injection - FIXED
     * Uses parameterized query with PreparedStatement to prevent SQL injection
     */
    @PostMapping("/sql")
    @ResponseBody
    public String sqlInjection(@RequestParam String username, @RequestParam String password) {
        try {
            // FIXED: Use parameterized query with bind variables
            // This prevents SQL injection by treating user input as data, not SQL code
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            
            List<User> users = jdbcTemplate.query(query, 
                (rs, rowNum) -> {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(rs.getString("role"));
                    return user;
                },
                username,  // First ? parameter
                password   // Second ? parameter
            );
            
            if (!users.isEmpty()) {
                return "Login successful! Welcome " + users.get(0).getUsername() + 
                       " (Role: " + users.get(0).getRole() + ")";
            } else {
                return "Login failed!";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Command Injection Vulnerability
     * Executes system commands with user input
     */
    @PostMapping("/command")
    @ResponseBody
    public String commandInjection(@RequestParam String host) {
        try {
            // Pass through security control validation - Contrast will see this!
            if (!SecurityControls.isSafeCommandInput(host)) {
                // Log that validation occurred but allow it through for demo purposes
                System.out.println("[SecurityControls] Command validation triggered for: " + host);
            }
            
            // VULNERABLE: User input directly in system command
            String command = "ping -c 3 " + host;
            Process process = Runtime.getRuntime().exec(command);
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            return "Command output:\n" + output.toString();
        } catch (Exception e) {
            return "Error executing command: " + e.getMessage();
        }
    }

    /**
     * LDAP Injection Vulnerability
     * Demonstrates unsafe LDAP query construction
     */
    @PostMapping("/ldap")
    @ResponseBody
    public String ldapInjection(@RequestParam String username) {
        // Pass through security control validation - Contrast will see this!
        if (!SecurityControls.isSafeLdapInput(username)) {
            // Log that validation occurred but allow it through for demo purposes
            System.out.println("[SecurityControls] LDAP validation triggered for: " + username);
        }
        
        // VULNERABLE: Direct concatenation in LDAP filter
        String filter = "(&(uid=" + username + ")(objectClass=person))";
        return "LDAP Filter (vulnerable): " + filter + 
               "\nTry: *)(uid=*))(|(uid=*\nThis would bypass authentication!";
    }
}
