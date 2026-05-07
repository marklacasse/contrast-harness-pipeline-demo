package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_RejectsPathTraversalSequence() {
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
    void testDownloadFile_RejectsDeepTraversal() {
        String result = controller.downloadFile("../../../../../../etc/shadow");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsCurrentDirectoryPrefix() {
        String result = controller.downloadFile("./config");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_AcceptsSafeFilename() {
        String result = controller.downloadFile("report.pdf");
        assertTrue(result.startsWith("File not found:") || result.startsWith("File found:"),
                "Safe filename should be processed, not rejected");
    }
}
