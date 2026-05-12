package com.contrast.demo.security;

import com.contrast.demo.controller.AccessControlController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessControlControllerSecurityTest {

    private final AccessControlController controller = new AccessControlController();

    @Test
    void testDownloadFile_PathTraversal_Rejected() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_AbsolutePath_Rejected() {
        String result = controller.downloadFile("/etc/passwd");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_HomeDirTraversal_Rejected() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_EmptyFilename_Rejected() {
        String result = controller.downloadFile("");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_DotSlashPrefix_Rejected() {
        String result = controller.downloadFile("./config");
        assertEquals("Access denied: invalid file path", result);
    }

    @Test
    void testDownloadFile_SafeFilename_Allowed() {
        String result = controller.downloadFile("report.txt");
        assertTrue(result.startsWith("File not found:") || result.startsWith("File found:"),
                "Safe filename should not be rejected by path validation");
    }
}
