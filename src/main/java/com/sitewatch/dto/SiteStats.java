package com.sitewatch.dto;

import com.sitewatch.util.Format;
import lombok.Value;

import java.time.Duration;
import java.util.Locale;

@Value
public class SiteStats {
    double uptime24h;
    double uptime7d;
    long checks24h;
    long checks7d;
    Double avgMs24h;
    Duration downtime7d;

    public String getUptime24hText() { return checks24h == 0 ? "—" : String.format(Locale.ROOT, "%.2f%%", uptime24h); }
    public String getUptime7dText() { return checks7d == 0 ? "—" : String.format(Locale.ROOT, "%.2f%%", uptime7d); }
    public String getUptime24hClass() { return level(uptime24h, checks24h); }
    public String getUptime7dClass() { return level(uptime7d, checks7d); }
    public String getAvgText() { return avgMs24h == null ? "—" : Math.round(avgMs24h) + " ms"; }
    public String getDowntimeText() { return Format.duration(downtime7d); }

    private static String level(double pct, long checks) {
        if (checks == 0) return "muted";
        return pct >= 99.0 ? "good" : pct >= 95.0 ? "warn" : "bad";
    }
}
