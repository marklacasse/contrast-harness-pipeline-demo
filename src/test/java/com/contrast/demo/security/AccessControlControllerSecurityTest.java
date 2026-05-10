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
    void testDownloadFile_PathTraversalWithDotDot_IsRejected() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Invalid filename: access denied", result);
    }

    @Test
    void testDownloadFile_AbsolutePath_IsRejected() {
        String result = controller.downloadFile("/etc/hosts");
        assertEquals("Invalid filename: access denied", result);
    }

    @Test
    void testDownloadFile_TildeHomePath_IsRejected() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Invalid filename: access denied", result);
    }

    @Test
    void testDownloadFile_RelativeDotSlash_IsRejected() {
        String result = controller.downloadFile("./config");
        assertEquals("Invalid filename: access denied", result);
    }

    @Test
    void testDownloadFile_MultipleTraversalSegments_IsRejected() {
        String result = controller.downloadFile("../../../etc/shadow");
        assertEquals("Invalid filename: access denied", result);
    }

    @Test
    void testDownloadFile_NullFilename_IsRejected() {
        String result = controller.downloadFile(null);
        assertEquals("Invalid filename: access denied", result);
    }

    @Test
    void testDownloadFile_EmptyFilename_IsRejected() {
        String result = controller.downloadFile("");
        assertEquals("Invalid filename: access denied", result);
    }

    @Test
    void testDownloadFile_SafeFilename_IsAllowed() {
        String result = controller.downloadFile("report.pdf");
        assertNotEquals("Invalid filename: access denied", result);
    }
}
