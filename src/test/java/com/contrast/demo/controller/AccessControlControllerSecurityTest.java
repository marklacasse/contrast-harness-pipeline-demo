package com.contrast.demo.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotDot() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotSlash() {
        String result = controller.downloadFile("./config");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsAbsolutePath() {
        String result = controller.downloadFile("/etc/passwd");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsTildeHomePath() {
        String result = controller.downloadFile("~/secrets");
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
    void testDownloadFile_AcceptsSafePathWithFolder() {
        String result = controller.downloadFile("docs/report.pdf");
        assertTrue(result.startsWith("File not found:") || result.startsWith("File found:"));
    }
}
