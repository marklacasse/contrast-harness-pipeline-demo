package com.contrast.demo.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Security Controls
 * 
 * These tests verify that validators and sanitizers work as expected.
 * Run with: mvn test -Dtest=SecurityControlsTest
 */
class SecurityControlsTest {

    // ========== SQL Injection Validator Tests ==========
    
    @Test
    void testIsSafeSqlInput_SafeInput() {
        assertTrue(SecurityControls.isSafeSqlInput("john_doe"));
        assertTrue(SecurityControls.isSafeSqlInput("user123"));
        assertTrue(SecurityControls.isSafeSqlInput("test@example.com"));
        assertTrue(SecurityControls.isSafeSqlInput(""));
        assertTrue(SecurityControls.isSafeSqlInput(null));
    }
    
    @Test
    void testIsSafeSqlInput_DangerousInput() {
        assertFalse(SecurityControls.isSafeSqlInput("' OR '1'='1"));
        assertFalse(SecurityControls.isSafeSqlInput("admin'--"));
        assertFalse(SecurityControls.isSafeSqlInput("1; DROP TABLE users"));
        assertFalse(SecurityControls.isSafeSqlInput("/* comment */"));
        assertFalse(SecurityControls.isSafeSqlInput("';exec('ls')"));
    }
    
    @Test
    void testIsSafeUsername_ValidUsernames() {
        assertTrue(SecurityControls.isSafeUsername("john"));
        assertTrue(SecurityControls.isSafeUsername("john_doe"));
        assertTrue(SecurityControls.isSafeUsername("user123"));
        assertTrue(SecurityControls.isSafeUsername("TEST_USER"));
    }
    
    @Test
    void testIsSafeUsername_InvalidUsernames() {
        assertFalse(SecurityControls.isSafeUsername("jo")); // Too short
        assertFalse(SecurityControls.isSafeUsername("this_is_a_very_long_username")); // Too long
        assertFalse(SecurityControls.isSafeUsername("john-doe")); // Hyphen not allowed
        assertFalse(SecurityControls.isSafeUsername("john@doe")); // @ not allowed
        assertFalse(SecurityControls.isSafeUsername("")); // Empty
        assertFalse(SecurityControls.isSafeUsername(null)); // Null
    }
    
    @Test
    void testIsNumeric_ValidNumbers() {
        assertTrue(SecurityControls.isNumeric("123"));
        assertTrue(SecurityControls.isNumeric("0"));
        assertTrue(SecurityControls.isNumeric("999999999"));
    }
    
    @Test
    void testIsNumeric_InvalidNumbers() {
        assertFalse(SecurityControls.isNumeric("abc"));
        assertFalse(SecurityControls.isNumeric("12.34"));
        assertFalse(SecurityControls.isNumeric(""));
        assertFalse(SecurityControls.isNumeric(null));
    }
    
    // ========== SQL Injection Sanitizer Tests ==========
    
    @Test
    void testSanitizeSqlInput_RemovesDangerousCharacters() {
        assertEquals("admin", SecurityControls.sanitizeSqlInput("admin'--"));
        assertEquals("1 DROP TABLE users", SecurityControls.sanitizeSqlInput("1; DROP TABLE users"));
        assertEquals("test  data", SecurityControls.sanitizeSqlInput("test /* comment */ data"));
        assertEquals("normal_user", SecurityControls.sanitizeSqlInput("normal_user"));
    }
    
    @Test
    void testSanitizeSqlInput_EscapesQuotes() {
        String input = "O'Brien";
        String result = SecurityControls.sanitizeSqlInput(input);
        assertEquals("O''Brien", result);
    }
    
    @Test
    void testSanitizeSqlInput_NullHandling() {
        assertNull(SecurityControls.sanitizeSqlInput(null));
    }
    
    // ========== XSS Validator Tests ==========
    
    @Test
    void testIsSafeHtmlInput_SafeText() {
        assertTrue(SecurityControls.isSafeHtmlInput("Hello World"));
        assertTrue(SecurityControls.isSafeHtmlInput("This is a comment"));
        assertTrue(SecurityControls.isSafeHtmlInput(""));
        assertTrue(SecurityControls.isSafeHtmlInput(null));
    }
    
    @Test
    void testIsSafeHtmlInput_DangerousInput() {
        assertFalse(SecurityControls.isSafeHtmlInput("<script>alert('XSS')</script>"));
        assertFalse(SecurityControls.isSafeHtmlInput("<img src=x onerror=alert(1)>"));
        assertFalse(SecurityControls.isSafeHtmlInput("javascript:alert(1)"));
        assertFalse(SecurityControls.isSafeHtmlInput("<div>content</div>"));
        assertFalse(SecurityControls.isSafeHtmlInput("test < 5"));
    }
    
    @Test
    void testIsSafeTextPattern_SafeText() {
        assertTrue(SecurityControls.isSafeTextPattern("Hello World"));
        assertTrue(SecurityControls.isSafeTextPattern("Test 123"));
        assertTrue(SecurityControls.isSafeTextPattern("Question?"));
        assertTrue(SecurityControls.isSafeTextPattern("email@example.com"));
    }
    
    @Test
    void testIsSafeTextPattern_UnsafeText() {
        assertFalse(SecurityControls.isSafeTextPattern("<script>"));
        assertFalse(SecurityControls.isSafeTextPattern("test{value}"));
        assertFalse(SecurityControls.isSafeTextPattern("price=$100"));
    }
    
    // ========== XSS Sanitizer Tests ==========
    
    @Test
    void testSanitizeHtmlOutput_EncodesSpecialCharacters() {
        assertEquals("&lt;script&gt;", SecurityControls.sanitizeHtmlOutput("<script>"));
        assertEquals("&lt;b&gt;bold&lt;&#x2F;b&gt;", SecurityControls.sanitizeHtmlOutput("<b>bold</b>"));
        assertEquals("Test &amp; More", SecurityControls.sanitizeHtmlOutput("Test & More"));
        assertEquals("&quot;quoted&quot;", SecurityControls.sanitizeHtmlOutput("\"quoted\""));
    }
    
    @Test
    void testSanitizeHtmlOutput_NullHandling() {
        assertNull(SecurityControls.sanitizeHtmlOutput(null));
    }
    
    @Test
    void testStripHtmlTags_RemovesTags() {
        assertEquals("bold", SecurityControls.stripHtmlTags("<b>bold</b>"));
        assertEquals("alert('XSS')", SecurityControls.stripHtmlTags("<script>alert('XSS')</script>"));
        assertEquals("Test  content", SecurityControls.stripHtmlTags("Test <div>content</div>"));
        assertEquals("Plain text", SecurityControls.stripHtmlTags("Plain text"));
    }
    
    // ========== Command Injection Validator Tests ==========
    
    @Test
    void testIsSafeCommandInput_SafeInput() {
        assertTrue(SecurityControls.isSafeCommandInput("google.com"));
        assertTrue(SecurityControls.isSafeCommandInput("192.168.1.1"));
        assertTrue(SecurityControls.isSafeCommandInput("test-host"));
        assertTrue(SecurityControls.isSafeCommandInput(""));
        assertTrue(SecurityControls.isSafeCommandInput(null));
    }
    
    @Test
    void testIsSafeCommandInput_DangerousInput() {
        assertFalse(SecurityControls.isSafeCommandInput("google.com; rm -rf /"));
        assertFalse(SecurityControls.isSafeCommandInput("host | cat /etc/passwd"));
        assertFalse(SecurityControls.isSafeCommandInput("host && ls"));
        assertFalse(SecurityControls.isSafeCommandInput("$(whoami)"));
        assertFalse(SecurityControls.isSafeCommandInput("`id`"));
    }
    
    @Test
    void testIsValidHost_ValidHosts() {
        assertTrue(SecurityControls.isValidHost("google.com"));
        assertTrue(SecurityControls.isValidHost("sub.domain.example.com"));
        assertTrue(SecurityControls.isValidHost("192.168.1.1"));
        assertTrue(SecurityControls.isValidHost("10.0.0.1"));
    }
    
    @Test
    void testIsValidHost_InvalidHosts() {
        assertFalse(SecurityControls.isValidHost("google.com; echo"));
        assertFalse(SecurityControls.isValidHost("host|cmd"));
        assertFalse(SecurityControls.isValidHost(""));
        assertFalse(SecurityControls.isValidHost(null));
        assertFalse(SecurityControls.isValidHost("999.999.999.999"));
    }
    
    // ========== Command Injection Sanitizer Tests ==========
    
    @Test
    void testSanitizeCommandInput_RemovesDangerousCharacters() {
        assertEquals("google.com", SecurityControls.sanitizeCommandInput("google.com"));
        assertEquals("google.com rm -rf /", SecurityControls.sanitizeCommandInput("google.com; rm -rf /"));
        assertEquals("host  cat /etc/passwd", SecurityControls.sanitizeCommandInput("host | cat /etc/passwd"));
    }
    
    // ========== Path Traversal Validator Tests ==========
    
    @Test
    void testIsSafePath_SafePaths() {
        assertTrue(SecurityControls.isSafePath("file.txt"));
        assertTrue(SecurityControls.isSafePath("folder/file.txt"));
        assertTrue(SecurityControls.isSafePath("docs/report.pdf"));
    }
    
    @Test
    void testIsSafePath_DangerousPaths() {
        assertFalse(SecurityControls.isSafePath("../etc/passwd"));
        assertFalse(SecurityControls.isSafePath("./config"));
        assertFalse(SecurityControls.isSafePath("/etc/passwd"));
        assertFalse(SecurityControls.isSafePath("~/secrets"));
        assertFalse(SecurityControls.isSafePath(""));
        assertFalse(SecurityControls.isSafePath(null));
    }
    
    @Test
    void testSanitizePath_RemovesTraversalSequences() {
        assertEquals("etcpasswd", SecurityControls.sanitizePath("../etc/passwd"));
        assertEquals("config", SecurityControls.sanitizePath("./config"));
        assertEquals("file.txt", SecurityControls.sanitizePath("../../../file.txt"));
    }
    
    // ========== LDAP Injection Validator Tests ==========
    
    @Test
    void testIsSafeLdapInput_SafeInput() {
        assertTrue(SecurityControls.isSafeLdapInput("john_doe"));
        assertTrue(SecurityControls.isSafeLdapInput("user123"));
        assertTrue(SecurityControls.isSafeLdapInput(""));
        assertTrue(SecurityControls.isSafeLdapInput(null));
    }
    
    @Test
    void testIsSafeLdapInput_DangerousInput() {
        assertFalse(SecurityControls.isSafeLdapInput("*"));
        assertFalse(SecurityControls.isSafeLdapInput("(cn=*)"));
        assertFalse(SecurityControls.isSafeLdapInput("user\\admin"));
        assertFalse(SecurityControls.isSafeLdapInput("admin/user"));
    }
    
    @Test
    void testSanitizeLdapInput_EscapesSpecialCharacters() {
        assertEquals("\\2a", SecurityControls.sanitizeLdapInput("*"));
        assertEquals("\\28cn=\\2a\\29", SecurityControls.sanitizeLdapInput("(cn=*)"));
        assertEquals("user\\5cadmin", SecurityControls.sanitizeLdapInput("user\\admin"));
    }
    
    // ========== Email Validator Tests ==========
    
    @Test
    void testIsValidEmail_ValidEmails() {
        assertTrue(SecurityControls.isValidEmail("test@example.com"));
        assertTrue(SecurityControls.isValidEmail("user.name@domain.co.uk"));
        assertTrue(SecurityControls.isValidEmail("first+last@company.org"));
    }
    
    @Test
    void testIsValidEmail_InvalidEmails() {
        assertFalse(SecurityControls.isValidEmail("not-an-email"));
        assertFalse(SecurityControls.isValidEmail("@example.com"));
        assertFalse(SecurityControls.isValidEmail("user@"));
        assertFalse(SecurityControls.isValidEmail(""));
        assertFalse(SecurityControls.isValidEmail(null));
    }
    
    // ========== URL Validator Tests ==========
    
    @Test
    void testIsSafeUrl_SafeUrls() {
        assertTrue(SecurityControls.isSafeUrl("http://example.com"));
        assertTrue(SecurityControls.isSafeUrl("https://secure.site.com"));
        assertTrue(SecurityControls.isSafeUrl("HTTP://EXAMPLE.COM"));
    }
    
    @Test
    void testIsSafeUrl_UnsafeUrls() {
        assertFalse(SecurityControls.isSafeUrl("file:///etc/passwd"));
        assertFalse(SecurityControls.isSafeUrl("ftp://server.com"));
        assertFalse(SecurityControls.isSafeUrl("javascript:alert(1)"));
        assertFalse(SecurityControls.isSafeUrl(""));
        assertFalse(SecurityControls.isSafeUrl(null));
    }
}
