package com.contrast.demo.controller;

import com.contrast.demo.security.SecurityControls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessControlControllerSecurityTest {

    @Test
    void testPathValidation_RejectsPathTraversalWithDotDot() {
        assertFalse(SecurityControls.isSafePath("../etc/passwd"));
    }

    @Test
    void testPathValidation_RejectsPathTraversalWithAbsolutePath() {
        assertFalse(SecurityControls.isSafePath("/etc/passwd"));
    }

    @Test
    void testPathValidation_RejectsPathTraversalWithTilde() {
        assertFalse(SecurityControls.isSafePath("~/secrets"));
    }

    @Test
    void testPathValidation_RejectsPathTraversalWithDotSlash() {
        assertFalse(SecurityControls.isSafePath("./config"));
    }

    @Test
    void testPathValidation_RejectsEmptyFilename() {
        assertFalse(SecurityControls.isSafePath(""));
    }

    @Test
    void testPathValidation_RejectsNullFilename() {
        assertFalse(SecurityControls.isSafePath(null));
    }

    @Test
    void testPathValidation_AcceptsSafeRelativePath() {
        assertTrue(SecurityControls.isSafePath("file.txt"));
    }

    @Test
    void testPathValidation_AcceptsSafeRelativePathWithFolder() {
        assertTrue(SecurityControls.isSafePath("docs/report.pdf"));
    }

    @Test
    void testPathValidation_AcceptsSafeRelativePathWithSubfolders() {
        assertTrue(SecurityControls.isSafePath("folder/subfolder/file.txt"));
    }
}
