package com.contrast.demo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    void downloadFile_rejectsPathTraversalWithDotDot() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Invalid filename", result);
    }

    @Test
    void downloadFile_rejectsAbsolutePath() {
        String result = controller.downloadFile("/etc/hosts");
        assertEquals("Invalid filename", result);
    }

    @Test
    void downloadFile_rejectsHomeDirTraversal() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Invalid filename", result);
    }

    @Test
    void downloadFile_rejectsDotSlashRelativePath() {
        String result = controller.downloadFile("./config");
        assertEquals("Invalid filename", result);
    }

    @Test
    void downloadFile_rejectsNestedTraversal() {
        String result = controller.downloadFile("../../etc/shadow");
        assertEquals("Invalid filename", result);
    }

    @Test
    void downloadFile_acceptsSafeFilename() {
        String result = controller.downloadFile("report.pdf");
        assertNotEquals("Invalid filename", result);
    }

    @Test
    void downloadFile_acceptsSafeNestedPath() {
        String result = controller.downloadFile("docs/report.pdf");
        assertNotEquals("Invalid filename", result);
    }
}
