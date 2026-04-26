package com.contrast.demo.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessControlControllerSecurityTest {

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotDot() {
        AccessControlController controller = new AccessControlController();
        
        String result = controller.downloadFile("../etc/passwd");
        
        assertEquals("Error: Invalid filename", result);
    }
    
    @Test
    void testDownloadFile_RejectsPathTraversalWithAbsolutePath() {
        AccessControlController controller = new AccessControlController();
        
        String result = controller.downloadFile("/etc/passwd");
        
        assertEquals("Error: Invalid filename", result);
    }
    
    @Test
    void testDownloadFile_RejectsPathTraversalWithDotSlash() {
        AccessControlController controller = new AccessControlController();
        
        String result = controller.downloadFile("./config");
        
        assertEquals("Error: Invalid filename", result);
    }
    
    @Test
    void testDownloadFile_RejectsPathTraversalWithTilde() {
        AccessControlController controller = new AccessControlController();
        
        String result = controller.downloadFile("~/secrets");
        
        assertEquals("Error: Invalid filename", result);
    }
    
    @Test
    void testDownloadFile_RejectsMultipleTraversalSequences() {
        AccessControlController controller = new AccessControlController();
        
        String result = controller.downloadFile("../../../etc/passwd");
        
        assertEquals("Error: Invalid filename", result);
    }
    
    @Test
    void testDownloadFile_AcceptsSafeFilename() {
        AccessControlController controller = new AccessControlController();
        
        String result = controller.downloadFile("document.txt");
        
        assertTrue(result.startsWith("File not found:") || result.startsWith("File found:"));
    }
    
    @Test
    void testDownloadFile_AcceptsSafeRelativePath() {
        AccessControlController controller = new AccessControlController();
        
        String result = controller.downloadFile("docs/report.pdf");
        
        assertTrue(result.startsWith("File not found:") || result.startsWith("File found:"));
    }
}
