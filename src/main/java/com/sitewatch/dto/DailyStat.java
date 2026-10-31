package com.sitewatch.dto;

import com.sitewatch.service.UptimeCalculator;
import lombok.Value;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Value
public class DailyStat {
    LocalDate date;
    long total;
    long upChecks;
    Double avgMs;

    public String getDateText() { return date.format(DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH)); }
    public long getDownChecks() { return total - upChecks; }
    public String getUptimeText() {
        return total == 0 ? "—" : String.format(Locale.ROOT, "%.2f%%", UptimeCalculator.percentage(upChecks, total));
    }
    public String getAvgText() { return avgMs == null ? "—" : Math.round(avgMs) + " ms"; }
}
