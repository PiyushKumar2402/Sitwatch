package com.sitewatch.service;

import com.sitewatch.dto.SiteRow;
import com.sitewatch.dto.SiteStats;
import com.sitewatch.model.CheckResult;
import com.sitewatch.model.Incident;
import com.sitewatch.model.Website;
import com.sitewatch.repo.CheckResultRepository;
import com.sitewatch.repo.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Uptime analytics: percentages, average response time, downtime duration. */
@Service
public class StatsService {

    private final CheckResultRepository checks;
    private final IncidentRepository incidents;

    public StatsService(CheckResultRepository checks, IncidentRepository incidents) {
        this.checks = checks;
        this.incidents = incidents;
    }

    public SiteStats compute(Website w) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime d1 = now.minusHours(24);
        LocalDateTime d7 = now.minusDays(7);

        long total24 = checks.countSince(w, d1);
        long up24 = checks.countUpSince(w, d1);
        long total7 = checks.countSince(w, d7);
        long up7 = checks.countUpSince(w, d7);
        Double avg = checks.avgResponseSince(w, d1);
        Duration down7 = UptimeCalculator.downtime(incidents.findOverlapping(w, d7), d7, now);

        return new SiteStats(UptimeCalculator.percentage(up24, total24), UptimeCalculator.percentage(up7, total7),
                total24, total7, avg, down7);
    }

    public SiteRow row(Website w) {
        List<String> strip = new ArrayList<>();
        for (CheckResult c : checks.findTop30ByWebsiteOrderByCheckedAtDesc(w)) strip.add(c.isUp() ? "u" : "d");
        Collections.reverse(strip);   // oldest on the left, newest on the right
        return new SiteRow(w, compute(w), strip);
    }
}
