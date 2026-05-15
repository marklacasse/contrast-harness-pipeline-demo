package com.contrast.demo.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_PathTraversalRejected() {
        String result = controller.downloadFile("../etc/passwd");
        assertTrue(result.startsWith("Invalid filename:"),
            "Path traversal attempt should be rejected");
    }

    @Test
    void testDownloadFile_AbsolutePathRejected() {
        String result = controller.downloadFile("/etc/hosts");
        assertTrue(result.startsWith("Invalid filename:"),
            "Absolute path should be rejected");
    }

    @Test
    void testDownloadFile_HomeDirectoryTraversalRejected() {
        String result = controller.downloadFile("~/secrets");
        assertTrue(result.startsWith("Invalid filename:"),
            "Home directory traversal should be rejected");
    }

    @Test
    void testDownloadFile_DotSlashRejected() {
        String result = controller.downloadFile("./config");
        assertTrue(result.startsWith("Invalid filename:"),
            "Dot-slash path should be rejected");
    }

    @Test
    void testDownloadFile_ValidFilenameNotRejected() {
        String result = controller.downloadFile("report.pdf");
        assertFalse(result.startsWith("Invalid filename:"),
            "Valid filename should not be rejected by the validator");
    }

    @Test
    void testDownloadFile_ValidSubdirectoryNotRejected() {
        String result = controller.downloadFile("docs/report.pdf");
        assertFalse(result.startsWith("Invalid filename:"),
            "Valid subdirectory path should not be rejected by the validator");
    }
}
