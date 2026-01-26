package com.contrast.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.*;

/**
 * OWASP A08:2021 - Software and Data Integrity Failures
 * This controller demonstrates deserialization and integrity vulnerabilities
 */
@Controller
@RequestMapping("/integrity")
public class IntegrityController {

    @GetMapping
    public String integrityHome(Model model) {
        return "integrity";
    }

    /**
     * Insecure Deserialization
     * Deserializes untrusted data
     */
    @PostMapping("/deserialize")
    @ResponseBody
    public String deserializeObject(@RequestParam String data) {
        try {
            // VULNERABLE: Deserializing untrusted data
            byte[] bytes = java.util.Base64.getDecoder().decode(data);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);
            Object obj = ois.readObject();
            
            return "Deserialized object: " + obj.toString() +
                   "\n\nThis is vulnerable to remote code execution!";
        } catch (Exception e) {
            return "Error deserializing: " + e.getMessage();
        }
    }

    /**
     * Unsafe JSON Deserialization
     * Uses unsafe JSON deserialization settings
     */
    @PostMapping("/json-deserialize")
    @ResponseBody
    public String jsonDeserialize(@RequestParam String json) {
        try {
            // VULNERABLE: May allow polymorphic type handling exploits
            ObjectMapper mapper = new ObjectMapper();
            mapper.enableDefaultTyping(); // Unsafe!
            
            Object obj = mapper.readValue(json, Object.class);
            return "JSON parsed: " + obj.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * No Integrity Check
     * Accepts file uploads without validation
     */
    @PostMapping("/upload")
    @ResponseBody
    public String uploadFile(@RequestParam String filename, 
                            @RequestParam String content) {
        try {
            // VULNERABLE: No integrity check, no signature verification
            File file = new File("/tmp/" + filename);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            
            return "File uploaded: " + file.getAbsolutePath() +
                   "\n\nNo integrity check performed!";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Unsigned Software Update
     * Accepts update packages without signature verification
     */
    @PostMapping("/update")
    @ResponseBody
    public String installUpdate(@RequestParam String updateUrl) {
        // VULNERABLE: No signature verification
        return "Installing update from: " + updateUrl +
               "\n\nWARNING: No signature verification!" +
               "\nAn attacker could supply a malicious update!";
    }
}
