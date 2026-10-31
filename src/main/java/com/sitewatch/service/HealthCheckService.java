package com.sitewatch.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/** Sends the HTTP GET and measures status code + response time. */
@Service
public class HealthCheckService {

    private final HttpClient client;
    private final Duration timeout;

    public HealthCheckService(@Value("${sitewatch.check.timeout-seconds:10}") int timeoutSeconds) {
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                // Redirects are NOT followed: a redirect to an internal address could bypass the SSRF check.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public CheckOutcome check(String url) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("User-Agent", "SiteWatch/1.0 (uptime monitor)")
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long ms = elapsedMs(start);
            int code = response.statusCode();
            boolean up = StatusClassifier.isUp(code);
            return new CheckOutcome(up, code, ms, up ? null : "HTTP " + code);
        } catch (HttpTimeoutException e) {
            return new CheckOutcome(false, null, elapsedMs(start), "Timeout after " + timeout.toSeconds() + "s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CheckOutcome(false, null, elapsedMs(start), "Check interrupted");
        } catch (Exception e) {
            return new CheckOutcome(false, null, elapsedMs(start), describe(e));
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String msg = root.getMessage() == null ? "" : ": " + root.getMessage();
        String text = root.getClass().getSimpleName() + msg;
        return text.length() > 300 ? text.substring(0, 300) : text;
    }
}
