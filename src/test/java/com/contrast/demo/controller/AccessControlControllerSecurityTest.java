package com.contrast.demo.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_SafeFilename() {
        String result = controller.downloadFile("document.txt");
        assertFalse(result.startsWith("Error: Invalid filename"));
    }

    @Test
    void testDownloadFile_PathTraversalWithDotDot() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_PathTraversalWithAbsolutePath() {
        String result = controller.downloadFile("/etc/passwd");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_PathTraversalWithTilde() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_PathTraversalWithDotSlash() {
        String result = controller.downloadFile("./config");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_PathTraversalMultipleDotDot() {
        String result = controller.downloadFile("../../../etc/passwd");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_EmptyFilename() {
        String result = controller.downloadFile("");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_NullFilename() {
        String result = controller.downloadFile(null);
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_ValidRelativePath() {
        String result = controller.downloadFile("folder/file.txt");
        assertFalse(result.startsWith("Error: Invalid filename"));
    }
}
