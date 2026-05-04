package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void testDownloadFile_TildePathRejected() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_EmptyFilenameRejected() {
        String result = controller.downloadFile("");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_DoubleEncodedTraversalRejected() {
        String result = controller.downloadFile("..%2Fetc%2Fpasswd");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_ValidFilenameDoesNotReturnAccessDenied() {
        String result = controller.downloadFile("report.txt");
        assertFalse(result.startsWith("Access denied:"),
                "A valid filename should not be rejected by path validation");
        assertTrue(result.startsWith("File not found:") || result.startsWith("File found:"),
                "A valid filename should return a file-not-found or file-found response");
    }
}
