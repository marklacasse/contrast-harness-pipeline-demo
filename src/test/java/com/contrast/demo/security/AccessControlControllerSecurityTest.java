package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for AccessControlController path traversal fix.
 */
class AccessControlControllerSecurityTest {

    private AccessControlController controller;

    @BeforeEach
    void setUp() {
        controller = new AccessControlController();
    }

    @Test
    void testDownloadFile_PathTraversalSequenceRejected() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_AbsolutePathRejected() {
        String result = controller.downloadFile("/etc/hosts");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_HomeDirectoryTraversalRejected() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_DeepTraversalRejected() {
        String result = controller.downloadFile("../../../etc/shadow");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_SafeFilenameNotRejected() {
        String result = controller.downloadFile("report.pdf");
        assertNotEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_SafeNestedPathNotRejected() {
        String result = controller.downloadFile("docs/report.pdf");
        assertNotEquals("Invalid filename", result);
    }
}
