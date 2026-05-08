package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_PathTraversalRejected() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_AbsolutePathRejected() {
        String result = controller.downloadFile("/etc/passwd");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_HomeDirectoryTraversalRejected() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_NullFilenameRejected() {
        String result = controller.downloadFile(null);
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_EmptyFilenameRejected() {
        String result = controller.downloadFile("");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_ValidFilenameNotRejected() {
        String result = controller.downloadFile("report.pdf");
        assertNotEquals("Invalid filename", result);
    }
}
