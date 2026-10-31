package com.sitewatch.service;

import com.sitewatch.model.Website;
import com.sitewatch.repo.CheckResultRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wakes up every few seconds and checks every website whose own interval has elapsed.
 * Checks run in parallel on a thread pool, so many sites never slow each other down.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sitewatch.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringScheduler {

    private final com.sitewatch.repo.WebsiteRepository websites;
    private final CheckResultRepository checks;
    private final MonitoringService monitoring;
    private final ExecutorService pool;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    private final int retentionDays;

    public MonitoringScheduler(com.sitewatch.repo.WebsiteRepository websites, CheckResultRepository checks,
                               MonitoringService monitoring,
                               @Value("${sitewatch.check.pool-size:10}") int poolSize,
                               @Value("${sitewatch.retention-days:30}") int retentionDays) {
        this.websites = websites;
        this.checks = checks;
        this.monitoring = monitoring;
        this.retentionDays = retentionDays;
        AtomicInteger n = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "site-check-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        for (Website w : websites.findByEnabledTrue()) {
            if (!isDue(w, now) || !inFlight.add(w.getId())) continue;
            Long id = w.getId();
            pool.execute(() -> {
                try {
                    monitoring.checkAndRecord(id);
                } catch (Exception e) {
                    log.error("Check failed unexpectedly for website {}", id, e);
                } finally {
                    inFlight.remove(id);
                }
            });
        }
    }

    static boolean isDue(Website w, LocalDateTime now) {
        return w.getLastCheckedAt() == null
                || !w.getLastCheckedAt().plusSeconds(w.getIntervalSeconds()).isAfter(now);
    }

    /** Housekeeping: delete history older than the retention period (default 30 days). */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeOldHistory() {
        int deleted = checks.deleteOlderThan(LocalDateTime.now().minusDays(retentionDays));
        log.info("Retention job removed {} old check records", deleted);
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }
}
