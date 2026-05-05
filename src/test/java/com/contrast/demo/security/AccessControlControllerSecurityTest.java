package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotDot() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsAbsolutePath() {
        String result = controller.downloadFile("/etc/hosts");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsHomeDirectoryTraversal() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsCurrentDirectoryPrefix() {
        String result = controller.downloadFile("./config");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsNestedTraversal() {
        String result = controller.downloadFile("../../etc/shadow");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_AcceptsSafeFilename() {
        String result = controller.downloadFile("report.pdf");
        assertFalse(result.equals("Invalid filename"), "Safe filename should not be rejected");
    }

    @Test
    void testDownloadFile_AcceptsSafeNestedFilename() {
        String result = controller.downloadFile("docs/report.pdf");
        assertFalse(result.equals("Invalid filename"), "Safe nested filename should not be rejected");
    }
}
