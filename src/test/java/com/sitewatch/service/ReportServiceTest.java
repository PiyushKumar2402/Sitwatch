package com.sitewatch.service;

import com.sitewatch.dto.DailyStat;
import com.sitewatch.model.CheckResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReportServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 10);

    private static CheckResult check(LocalDate day, boolean up, Long ms) {
        CheckResult c = new CheckResult();
        c.setCheckedAt(day.atTime(10, 0));
        c.setUp(up);
        c.setResponseTimeMs(ms);
        return c;
    }

    @Test
    void groupsChecksPerDayOldestFirstAndIncludesEmptyDays() {
        List<CheckResult> data = List.of(
                check(TODAY, true, 100L), check(TODAY, true, 300L), check(TODAY, false, null),
                check(TODAY.minusDays(2), true, 50L));

        List<DailyStat> days = ReportService.buildDaily(data, TODAY, 3);

        assertEquals(3, days.size());
        assertEquals(TODAY.minusDays(2), days.get(0).getDate());
        assertEquals(1, days.get(0).getTotal());
        assertEquals(0, days.get(1).getTotal());          // yesterday: no data
        assertEquals("—", days.get(1).getUptimeText());
        assertEquals(3, days.get(2).getTotal());
        assertEquals(1, days.get(2).getDownChecks());
        assertEquals("66.67%", days.get(2).getUptimeText());
        assertEquals(200.0, days.get(2).getAvgMs());      // average of successful checks only
    }

    @Test
    void dayWithOnlyFailuresHasNoAverage() {
        List<DailyStat> days = ReportService.buildDaily(List.of(check(TODAY, false, null)), TODAY, 1);
        assertNull(days.get(0).getAvgMs());
        assertEquals("0.00%", days.get(0).getUptimeText());
    }
}
