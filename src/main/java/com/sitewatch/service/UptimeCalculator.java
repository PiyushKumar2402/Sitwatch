package com.sitewatch.service;

import com.sitewatch.model.Incident;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public final class UptimeCalculator {
    private UptimeCalculator() {}

    /** Uptime percentage rounded to 2 decimals. Returns 0 when there are no checks yet. */
    public static double percentage(long upChecks, long totalChecks) {
        if (totalChecks <= 0) return 0.0;
        return Math.round(upChecks * 10000.0 / totalChecks) / 100.0;
    }

    /** Total downtime inside [from, to], clipping incidents that start earlier or are still open. */
    public static Duration downtime(List<Incident> incidents, LocalDateTime from, LocalDateTime to) {
        Duration sum = Duration.ZERO;
        for (Incident i : incidents) {
            LocalDateTime start = i.getStartedAt().isBefore(from) ? from : i.getStartedAt();
            LocalDateTime end = (i.getResolvedAt() == null || i.getResolvedAt().isAfter(to)) ? to : i.getResolvedAt();
            if (end.isAfter(start)) sum = sum.plus(Duration.between(start, end));
        }
        return sum;
    }
}
