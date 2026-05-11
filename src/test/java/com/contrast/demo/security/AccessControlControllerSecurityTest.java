package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_PathTraversalSequenceRejected() {
        String result = controller.downloadFile("../../../etc/passwd");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_AbsolutePathRejected() {
        String result = controller.downloadFile("/etc/hosts");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_HomeDirTraversalRejected() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_DotSlashTraversalRejected() {
        String result = controller.downloadFile("./config");
        assertEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_ValidFilenameNotRejected() {
        String result = controller.downloadFile("report.pdf");
        assertNotEquals("Invalid filename", result);
    }

    @Test
    void testDownloadFile_ValidSubpathNotRejected() {
        String result = controller.downloadFile("docs/report.pdf");
        assertNotEquals("Invalid filename", result);
    }
}
