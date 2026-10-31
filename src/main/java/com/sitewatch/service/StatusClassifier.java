package com.sitewatch.service;

/** Decides whether an HTTP status code means "the site is up". */
public final class StatusClassifier {
    private StatusClassifier() {}

    /** 2xx and 3xx count as up. Redirects are not followed (see HealthCheckService), so a 301/302 is a healthy answer. */
    public static boolean isUp(int statusCode) {
        return statusCode >= 200 && statusCode < 400;
    }
}
