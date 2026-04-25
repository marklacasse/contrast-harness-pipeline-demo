package com.contrast.demo.controller;

import com.contrast.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessControlControllerSecurityTest {

    private AccessControlController controller;

    @BeforeEach
    void setUp() {
        controller = new AccessControlController();
    }

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotDot() {
        String result = controller.downloadFile("../etc/passwd");
        
        assertEquals("Error: Invalid file path", result);
    }

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotSlash() {
        String result = controller.downloadFile("./config");
        
        assertEquals("Error: Invalid file path", result);
    }

    @Test
    void testDownloadFile_RejectsAbsolutePath() {
        String result = controller.downloadFile("/etc/passwd");
        
        assertEquals("Error: Invalid file path", result);
    }

    @Test
    void testDownloadFile_RejectsTildeHomePath() {
        String result = controller.downloadFile("~/secrets");
        
        assertEquals("Error: Invalid file path", result);
    }

    @Test
    void testDownloadFile_RejectsMultipleTraversalSequences() {
        String result = controller.downloadFile("../../../etc/passwd");
        
        assertEquals("Error: Invalid file path", result);
    }

    @Test
    void testDownloadFile_RejectsEmptyPath() {
        String result = controller.downloadFile("");
        
        assertEquals("Error: Invalid file path", result);
    }

    @Test
    void testDownloadFile_AcceptsSafeRelativePath() {
        String result = controller.downloadFile("file.txt");
        
        assertNotEquals("Error: Invalid file path", result);
    }

    @Test
    void testDownloadFile_AcceptsSafePathWithFolder() {
        String result = controller.downloadFile("docs/report.pdf");
        
        assertNotEquals("Error: Invalid file path", result);
    }
}
