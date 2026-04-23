package com.contrast.demo.controller;

import com.contrast.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security Tests for AccessControlController
 * 
 * These tests verify that path traversal vulnerabilities are properly mitigated.
 */
class AccessControlControllerSecurityTest {

    private AccessControlController controller;

    @BeforeEach
    void setUp() {
        controller = new AccessControlController();
    }

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotDot() {
        String result = controller.downloadFile("../etc/passwd");
        
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsPathTraversalWithAbsolutePath() {
        String result = controller.downloadFile("/etc/passwd");
        
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotSlash() {
        String result = controller.downloadFile("./config");
        
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsPathTraversalWithTilde() {
        String result = controller.downloadFile("~/secrets");
        
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsMultipleTraversalSequences() {
        String result = controller.downloadFile("../../../etc/passwd");
        
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsEmptyFilename() {
        String result = controller.downloadFile("");
        
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_AcceptsSafeFilename() {
        String result = controller.downloadFile("document.txt");
        
        assertTrue(result.startsWith("File not found:") || result.startsWith("File found:"));
    }

    @Test
    void testDownloadFile_AcceptsSafeRelativePath() {
        String result = controller.downloadFile("folder/file.txt");
        
        assertTrue(result.startsWith("File not found:") || result.startsWith("File found:"));
    }
}
