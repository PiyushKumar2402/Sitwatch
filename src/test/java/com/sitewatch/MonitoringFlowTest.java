package com.sitewatch;

import com.sitewatch.model.AppUser;
import com.sitewatch.model.Incident;
import com.sitewatch.model.SiteStatus;
import com.sitewatch.model.Website;
import com.sitewatch.repo.CheckResultRepository;
import com.sitewatch.repo.IncidentRepository;
import com.sitewatch.repo.UserRepository;
import com.sitewatch.repo.WebsiteRepository;
import com.sitewatch.service.MonitoringService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end test of the core logic: site works, breaks (incident opens), recovers (incident closes). */
@SpringBootTest(properties = {
        "sitewatch.scheduler.enabled=false",
        "sitewatch.allow-private-hosts=true",
        "spring.datasource.url=jdbc:h2:mem:flowtest;MODE=MySQL;DB_CLOSE_DELAY=-1"})
class MonitoringFlowTest {

    @Autowired UserRepository users;
    @Autowired WebsiteRepository websites;
    @Autowired CheckResultRepository checks;
    @Autowired IncidentRepository incidents;
    @Autowired MonitoringService monitoring;

    private HttpServer server;
    private final AtomicInteger responseCode = new AtomicInteger(200);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> { ex.sendResponseHeaders(responseCode.get(), -1); ex.close(); });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void detectsOutageAndRecovery() {
        AppUser owner = new AppUser();
        owner.setUsername("flow-owner");
        owner.setPasswordHash("not-used");
        users.save(owner);

        Website w = new Website();
        w.setOwner(owner);
        w.setName("local test site");
        w.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        Long id = websites.save(w).getId();

        // 1. healthy
        monitoring.checkAndRecord(id);
        Website after = websites.findById(id).orElseThrow();
        assertEquals(SiteStatus.UP, after.getLastStatus());
        assertEquals(200, after.getLastStatusCode());
        assertTrue(incidents.findFirstByWebsiteAndResolvedAtIsNull(after).isEmpty());

        // 2. breaks: first failure opens exactly one incident, repeated failures do not open more
        responseCode.set(503);
        monitoring.checkAndRecord(id);
        monitoring.checkAndRecord(id);
        after = websites.findById(id).orElseThrow();
        assertEquals(SiteStatus.DOWN, after.getLastStatus());
        assertEquals(1, incidents.findTop20ByWebsiteOrderByStartedAtDesc(after).size());
        assertTrue(incidents.findFirstByWebsiteAndResolvedAtIsNull(after).isPresent());

        // 3. recovers: the incident is closed
        responseCode.set(200);
        monitoring.checkAndRecord(id);
        after = websites.findById(id).orElseThrow();
        assertEquals(SiteStatus.UP, after.getLastStatus());
        Incident closed = incidents.findTop20ByWebsiteOrderByStartedAtDesc(after).get(0);
        assertNotNull(closed.getResolvedAt());
        assertEquals("HTTP 503", closed.getReason());

        // every check was stored in the history
        assertEquals(4, checks.countSince(after, java.time.LocalDateTime.now().minusMinutes(5)));
    }

    @Test
    void pausedWebsitesAreNotChecked() {
        AppUser owner = new AppUser();
        owner.setUsername("flow-owner-2");
        owner.setPasswordHash("not-used");
        users.save(owner);
        Website w = new Website();
        w.setOwner(owner);
        w.setName("paused");
        w.setUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        w.setEnabled(false);
        Long id = websites.save(w).getId();

        monitoring.checkAndRecord(id);

        assertEquals(SiteStatus.UNKNOWN, websites.findById(id).orElseThrow().getLastStatus());
    }
}
