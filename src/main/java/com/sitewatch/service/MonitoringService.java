package com.sitewatch.service;

import com.sitewatch.model.CheckResult;
import com.sitewatch.model.Incident;
import com.sitewatch.model.SiteStatus;
import com.sitewatch.model.Website;
import com.sitewatch.repo.CheckResultRepository;
import com.sitewatch.repo.IncidentRepository;
import com.sitewatch.repo.WebsiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * The core monitoring logic: probe a site, store the result, open/close incidents, raise alerts.
 * The HTTP call runs OUTSIDE the database transaction so a slow site never holds a DB connection.
 */
@Service
public class MonitoringService {

    private final WebsiteRepository websites;
    private final CheckResultRepository checks;
    private final IncidentRepository incidents;
    private final HealthCheckService health;
    private final UrlValidator validator;
    private final AlertService alerts;
    private final TransactionTemplate tx;

    public MonitoringService(WebsiteRepository websites, CheckResultRepository checks, IncidentRepository incidents,
                             HealthCheckService health, UrlValidator validator, AlertService alerts,
                             TransactionTemplate tx) {
        this.websites = websites;
        this.checks = checks;
        this.incidents = incidents;
        this.health = health;
        this.validator = validator;
        this.alerts = alerts;
        this.tx = tx;
    }

    /** What changed after recording a check (null = nothing to alert about). */
    private record Transition(boolean wentDown, Website website, Incident incident) {}

    public void checkAndRecord(Long websiteId) {
        Website w = websites.findById(websiteId).orElse(null);
        if (w == null || !w.isEnabled()) return;

        CheckOutcome outcome;
        try {
            validator.validate(w.getUrl());   // re-checked on every probe (DNS can change after the site was added)
            outcome = health.check(w.getUrl());
        } catch (IllegalArgumentException e) {
            outcome = new CheckOutcome(false, null, 0, e.getMessage());
        }

        CheckOutcome result = outcome;
        Transition t = tx.execute(status -> record(websiteId, result));
        if (t != null) {
            if (t.wentDown()) alerts.siteDown(t.website(), t.incident());
            else alerts.siteRecovered(t.website(), t.incident());
        }
    }

    private Transition record(Long websiteId, CheckOutcome o) {
        Website w = websites.findById(websiteId).orElse(null);
        if (w == null) return null;          // deleted while the check was running
        LocalDateTime now = LocalDateTime.now();

        CheckResult r = new CheckResult();
        r.setWebsite(w);
        r.setCheckedAt(now);
        r.setUp(o.up());
        r.setStatusCode(o.statusCode());
        r.setResponseTimeMs(o.responseMs());
        r.setError(o.error());
        checks.save(r);

        w.setLastStatus(o.up() ? SiteStatus.UP : SiteStatus.DOWN);
        w.setLastCheckedAt(now);
        w.setLastResponseMs(o.up() ? o.responseMs() : null);
        w.setLastStatusCode(o.statusCode());
        w.setLastError(o.error());
        websites.save(w);

        Optional<Incident> open = incidents.findFirstByWebsiteAndResolvedAtIsNull(w);
        if (!o.up() && open.isEmpty()) {
            Incident i = new Incident();
            i.setWebsite(w);
            i.setStartedAt(now);
            i.setReason(o.error());
            return new Transition(true, w, incidents.save(i));
        }
        if (o.up() && open.isPresent()) {
            Incident i = open.get();
            i.setResolvedAt(now);
            return new Transition(false, w, incidents.save(i));
        }
        return null;
    }
}
