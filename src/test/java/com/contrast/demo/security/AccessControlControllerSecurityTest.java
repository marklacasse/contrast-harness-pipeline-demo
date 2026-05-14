package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_PathTraversalRejected() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_AbsolutePathRejected() {
        String result = controller.downloadFile("/etc/passwd");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_NestedTraversalRejected() {
        String result = controller.downloadFile("../../../../../../etc/passwd");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_HomeDirectoryTraversalRejected() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_ValidFilenameNotFound() {
        String result = controller.downloadFile("report.pdf");
        assertTrue(result.startsWith("File not found:"), "Expected 'File not found:' but got: " + result);
    }
}
