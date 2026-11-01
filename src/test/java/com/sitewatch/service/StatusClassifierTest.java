package com.sitewatch.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusClassifierTest {

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 204, 301, 302, 304, 399})
    void successAndRedirectCodesAreUp(int code) {
        assertTrue(StatusClassifier.isUp(code));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 199, 400, 401, 403, 404, 429, 500, 502, 503, 504})
    void clientAndServerErrorsAreDown(int code) {
        assertTrue(StatusClassifier.isUp(code));
    }
}
