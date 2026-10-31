package com.sitewatch.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/** Uses a tiny local HTTP server, so the tests never depend on real websites or internet access. */
class HealthCheckServiceTest {

    private HttpServer server;
    private String base;
    private final HealthCheckService service = new HealthCheckService(1);   // 1 second timeout

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/ok", ex -> { ex.sendResponseHeaders(200, -1); ex.close(); });
        server.createContext("/boom", ex -> { ex.sendResponseHeaders(500, -1); ex.close(); });
        server.createContext("/missing", ex -> { ex.sendResponseHeaders(404, -1); ex.close(); });
        server.createContext("/redirect", ex -> {
            ex.getResponseHeaders().add("Location", "/ok");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        server.createContext("/slow", ex -> {
            try { Thread.sleep(3000); ex.sendResponseHeaders(200, -1); } catch (Exception ignored) { }
            finally { ex.close(); }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void healthySiteIsUpWithStatusAndTiming() {
        CheckOutcome o = service.check(base + "/ok");
        assertTrue(o.up());
        assertEquals(200, o.statusCode());
        assertNull(o.error());
        assertTrue(o.responseMs() >= 0);
    }

    @Test
    void serverErrorIsDown() {
        CheckOutcome o = service.check(base + "/boom");
        assertFalse(o.up());
        assertEquals(500, o.statusCode());
        assertEquals("HTTP 500", o.error());
    }

    @Test
    void notFoundIsDown() {
        assertFalse(service.check(base + "/missing").up());
    }

    @Test
    void redirectsAreNotFollowedAndCountAsUp() {
        CheckOutcome o = service.check(base + "/redirect");
        assertTrue(o.up());
        assertEquals(302, o.statusCode());
    }

    @Test
    void slowSiteTimesOut() {
        CheckOutcome o = service.check(base + "/slow");
        assertFalse(o.up());
        assertNull(o.statusCode());
        assertTrue(o.error().startsWith("Timeout"), o.error());
    }

    @Test
    void closedPortIsDownWithAnError() {
        CheckOutcome o = service.check("http://127.0.0.1:1/");
        assertFalse(o.up());
        assertNull(o.statusCode());
        assertNotNull(o.error());
    }
}
