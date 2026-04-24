package com.contrast.demo.controller;

import com.contrast.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccessControlControllerSecurityTest {

    @InjectMocks
    private AccessControlController controller;

    @Mock
    private UserRepository userRepository;

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotDot() {
        String result = controller.downloadFile("../etc/passwd");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsPathTraversalWithDotSlash() {
        String result = controller.downloadFile("./config");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsAbsolutePath() {
        String result = controller.downloadFile("/etc/passwd");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsHomeDirectory() {
        String result = controller.downloadFile("~/secrets");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_RejectsEmptyFilename() {
        String result = controller.downloadFile("");
        assertEquals("Error: Invalid filename", result);
    }

    @Test
    void testDownloadFile_AcceptsSafeFilename() {
        String result = controller.downloadFile("document.txt");
        assertTrue(result.contains("File not found") || result.contains("File found"));
    }

    @Test
    void testDownloadFile_AcceptsSafePathWithFolder() {
        String result = controller.downloadFile("docs/report.pdf");
        assertTrue(result.contains("File not found") || result.contains("File found"));
    }
}
