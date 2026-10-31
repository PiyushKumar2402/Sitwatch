package com.sitewatch.service;

import com.sitewatch.model.Incident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UptimeCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 10, 12, 0, 0);

    private static Incident incident(LocalDateTime start, LocalDateTime end) {
        Incident i = new Incident();
        i.setStartedAt(start);
        i.setResolvedAt(end);
        return i;
    }

    @Test
    void percentageIsRoundedToTwoDecimals() {
        assertEquals(99.9, UptimeCalculator.percentage(999, 1000));
        assertEquals(33.33, UptimeCalculator.percentage(1, 3));
        assertEquals(100.0, UptimeCalculator.percentage(50, 50));
        assertEquals(0.0, UptimeCalculator.percentage(0, 10));
    }

    @Test
    void percentageWithNoChecksIsZero() {
        assertEquals(0.0, UptimeCalculator.percentage(0, 0));
    }

    @Test
    void downtimeSumsResolvedIncidents() {
        List<Incident> list = List.of(
                incident(NOW.minusHours(5), NOW.minusHours(4)),
                incident(NOW.minusHours(2), NOW.minusMinutes(90)));
        assertEquals(Duration.ofMinutes(90), UptimeCalculator.downtime(list, NOW.minusDays(1), NOW));
    }

    @Test
    void incidentStartingBeforeTheWindowIsClipped() {
        List<Incident> list = List.of(incident(NOW.minusHours(30), NOW.minusHours(23)));
        assertEquals(Duration.ofHours(1), UptimeCalculator.downtime(list, NOW.minusHours(24), NOW));
    }

    @Test
    void ongoingIncidentCountsUntilNow() {
        List<Incident> list = List.of(incident(NOW.minusMinutes(10), null));
        assertEquals(Duration.ofMinutes(10), UptimeCalculator.downtime(list, NOW.minusDays(1), NOW));
    }
}
