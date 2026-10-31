package com.sitewatch.service;

import com.sitewatch.dto.WebsiteForm;
import com.sitewatch.model.AppUser;
import com.sitewatch.model.Website;
import com.sitewatch.repo.CheckResultRepository;
import com.sitewatch.repo.IncidentRepository;
import com.sitewatch.repo.WebsiteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Service
public class WebsiteService {

    private final WebsiteRepository websites;
    private final CheckResultRepository checks;
    private final IncidentRepository incidents;
    private final UrlValidator validator;
    private final int minInterval;
    private final int maxPerUser;

    public WebsiteService(WebsiteRepository websites, CheckResultRepository checks, IncidentRepository incidents,
                          UrlValidator validator,
                          @Value("${sitewatch.min-interval-seconds:30}") int minInterval,
                          @Value("${sitewatch.max-websites-per-user:25}") int maxPerUser) {
        this.websites = websites;
        this.checks = checks;
        this.incidents = incidents;
        this.validator = validator;
        this.minInterval = minInterval;
        this.maxPerUser = maxPerUser;
    }

    public List<Website> list(AppUser owner) {
        return websites.findByOwnerOrderByNameAsc(owner);
    }

    public Website get(Long id, AppUser owner) {
        return websites.findByIdAndOwner(id, owner).orElseThrow(() -> new NotFoundException("Website not found"));
    }

    @Transactional
    public Website add(AppUser owner, WebsiteForm form) {
        if (websites.countByOwner(owner) >= maxPerUser)
            throw new IllegalArgumentException("You can monitor at most " + maxPerUser + " websites.");
        Website w = new Website();
        w.setOwner(owner);
        apply(w, form);
        return websites.save(w);
    }

    @Transactional
    public Website update(Long id, AppUser owner, WebsiteForm form) {
        Website w = get(id, owner);
        apply(w, form);
        return websites.save(w);
    }

    @Transactional
    public void toggle(Long id, AppUser owner) {
        Website w = get(id, owner);
        w.setEnabled(!w.isEnabled());
        websites.save(w);
    }

    @Transactional
    public void delete(Long id, AppUser owner) {
        Website w = get(id, owner);
        checks.deleteByWebsite(w);
        incidents.deleteByWebsite(w);
        websites.delete(w);
    }

    private void apply(Website w, WebsiteForm form) {
        int interval = form.getIntervalSeconds() == null ? 60 : form.getIntervalSeconds();
        if (interval < minInterval)
            throw new IllegalArgumentException("The check interval must be at least " + minInterval + " seconds.");
        String url = validator.validate(form.getUrl());
        String name = form.getName() == null ? "" : form.getName().trim();
        if (name.isEmpty()) name = URI.create(url).getHost();
        w.setUrl(url);
        w.setName(name);
        w.setIntervalSeconds(interval);
    }
}
