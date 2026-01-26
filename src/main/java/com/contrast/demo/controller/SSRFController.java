package com.contrast.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * OWASP A10:2021 - Server-Side Request Forgery (SSRF)
 * This controller demonstrates SSRF vulnerabilities
 */
@Controller
@RequestMapping("/ssrf")
public class SSRFController {

    @GetMapping
    public String ssrfHome(Model model) {
        return "ssrf";
    }

    /**
     * URL Fetch without Validation
     * Fetches content from user-supplied URL
     */
    @PostMapping("/fetch-url")
    @ResponseBody
    public String fetchUrl(@RequestParam String url) {
        try {
            // VULNERABLE: No validation of URL
            URL targetUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();
            
            return "Fetched content from: " + url + "\n\n" + 
                   response.substring(0, Math.min(500, response.length()));
        } catch (Exception e) {
            return "Error fetching URL: " + e.getMessage();
        }
    }

    /**
     * Image Proxy
     * Proxies images from user-supplied URLs
     */
    @GetMapping("/proxy-image")
    @ResponseBody
    public String proxyImage(@RequestParam String imageUrl) {
        try {
            // VULNERABLE: Can be used to scan internal network
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();
            
            return "Image URL: " + imageUrl + 
                   "\nResponse Code: " + responseCode +
                   "\nContent-Type: " + connection.getContentType() +
                   "\n\nTry: http://localhost:8080/actuator/health" +
                   "\nOr: http://169.254.169.254/latest/meta-data/";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * XXE (XML External Entity) Vulnerability
     * Processes XML with external entities enabled
     */
    @PostMapping("/parse-xml")
    @ResponseBody
    public String parseXml(@RequestParam String xmlContent) {
        try {
            // VULNERABLE: XXE enabled
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // These should be disabled for security, but we're leaving them on
            // factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            
            return "XML parsed successfully!\n" +
                   "Root element: " + doc.getDocumentElement().getNodeName() +
                   "\n\nTry XXE payload like:\n" +
                   "<?xml version=\"1.0\"?>\n" +
                   "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n" +
                   "<root>&xxe;</root>";
        } catch (Exception e) {
            return "Error parsing XML: " + e.getMessage();
        }
    }

    /**
     * DNS Rebinding Vulnerability
     * No validation of redirect targets
     */
    @PostMapping("/webhook")
    @ResponseBody
    public String triggerWebhook(@RequestParam String webhookUrl) {
        try {
            // VULNERABLE: Allows requests to internal services
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.getOutputStream().write("webhook triggered".getBytes());
            
            int responseCode = connection.getResponseCode();
            return "Webhook triggered: " + webhookUrl + 
                   "\nResponse: " + responseCode;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
