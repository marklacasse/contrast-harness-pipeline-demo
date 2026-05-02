package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Security tests for path traversal fix in AccessControlController#downloadFile
 */
class AccessControlControllerSecurityTest {

    private AccessControlController controller;

    @BeforeEach
    void setUp() {
        controller = new AccessControlController();
    }

    @Test
    void testDownloadFile_AbsolutePath_IsRejected() {
        String result = controller.downloadFile("/etc/hosts");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_ParentDirectoryTraversal_IsRejected() {
        String result = controller.downloadFile("../../etc/passwd");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_HomeDirectoryTraversal_IsRejected() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_RelativeCurrentDir_IsRejected() {
        String result = controller.downloadFile("./config");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_NullFilename_IsRejected() {
        String result = controller.downloadFile(null);
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_EmptyFilename_IsRejected() {
        String result = controller.downloadFile("");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_ValidFilename_IsNotRejected() {
        String result = controller.downloadFile("report.pdf");
        assertNotEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_ValidNestedPath_IsNotRejected() {
        String result = controller.downloadFile("docs/report.pdf");
        assertNotEquals("Invalid filename", result);
    }
}
