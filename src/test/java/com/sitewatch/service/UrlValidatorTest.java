package com.sitewatch.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorTest {

    private final UrlValidator strict = new UrlValidator(false);

    @Test
    void addsHttpsWhenSchemeIsMissing() {
        assertEquals("https://8.8.8.8/status", strict.validate("8.8.8.8/status"));
    }

    @Test
    void keepsExplicitHttp() {
        assertEquals("http://8.8.8.8", strict.validate("  http://8.8.8.8  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://127.0.0.1", "http://127.0.0.1:8080/admin", "http://10.0.0.5", "http://192.168.1.1",
            "http://172.16.0.9", "http://169.254.169.254/latest/meta-data", "http://[::1]/", "http://localhost"})
    void blocksLocalAndPrivateTargets(String url) {
        assertThrows(IllegalArgumentException.class, () -> strict.validate(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ftp://8.8.8.8", "file:///etc/passwd", "http://user:pw@8.8.8.8", "http://"})
    void rejectsInvalidInput(String url) {
        assertThrows(IllegalArgumentException.class, () -> strict.validate(url));
    }

    @Test
    void privateHostsCanBeAllowedForLocalDemos() {
        assertEquals("http://127.0.0.1:8080", new UrlValidator(true).validate("http://127.0.0.1:8080"));
    }

    @Test
    void isPrivateRecognisesSpecialRanges() throws Exception {
        assertTrue(UrlValidator.isPrivate(InetAddress.getByName("100.64.0.1")));
        assertTrue(UrlValidator.isPrivate(InetAddress.getByName("fd00::1")));
        assertFalse(UrlValidator.isPrivate(InetAddress.getByName("8.8.8.8")));
    }
}
