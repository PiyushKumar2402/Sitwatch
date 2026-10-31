package com.sitewatch.service;

import com.sitewatch.dto.DailyStat;
import com.sitewatch.model.CheckResult;
import com.sitewatch.model.Website;
import com.sitewatch.repo.CheckResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Daily / weekly report for one website. */
@Service
public class ReportService {

    private final CheckResultRepository checks;

    public ReportService(CheckResultRepository checks) {
        this.checks = checks;
    }

    public List<DailyStat> lastDays(Website w, int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(days - 1L).atStartOfDay();
        return buildDaily(checks.findByWebsiteAndCheckedAtGreaterThanEqualOrderByCheckedAtAsc(w, since), today, days);
    }

    /** Pure function (easy to unit test): groups checks by calendar day, oldest day first, empty days included. */
    public static List<DailyStat> buildDaily(List<CheckResult> results, LocalDate today, int days) {
        Map<LocalDate, List<CheckResult>> byDay =
                results.stream().collect(Collectors.groupingBy(c -> c.getCheckedAt().toLocalDate()));
        List<DailyStat> out = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<CheckResult> list = byDay.getOrDefault(day, List.of());
            long up = list.stream().filter(CheckResult::isUp).count();
            Double avg = list.stream().filter(c -> c.isUp() && c.getResponseTimeMs() != null)
                    .mapToLong(CheckResult::getResponseTimeMs).average().stream().boxed().findFirst().orElse(null);
            out.add(new DailyStat(day, list.size(), up, avg));
        }
        return out;
    }
}
