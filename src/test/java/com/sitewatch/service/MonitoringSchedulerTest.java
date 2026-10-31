package com.sitewatch.service;

import com.sitewatch.model.Website;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonitoringSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 10, 12, 0, 0);

    private static Website site(LocalDateTime lastChecked, int interval) {
        Website w = new Website();
        w.setLastCheckedAt(lastChecked);
        w.setIntervalSeconds(interval);
        return w;
    }

    @Test
    void neverCheckedSiteIsDue() {
        assertTrue(MonitoringScheduler.isDue(site(null, 60), NOW));
    }

    @Test
    void siteIsDueOnlyAfterItsOwnInterval() {
        assertFalse(MonitoringScheduler.isDue(site(NOW.minusSeconds(30), 60), NOW));
        assertTrue(MonitoringScheduler.isDue(site(NOW.minusSeconds(60), 60), NOW));
        assertTrue(MonitoringScheduler.isDue(site(NOW.minusSeconds(61), 60), NOW));
        assertFalse(MonitoringScheduler.isDue(site(NOW.minusSeconds(200), 300), NOW));
    }
}
