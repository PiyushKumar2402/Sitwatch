package com.sitewatch.service;

import com.sitewatch.model.Incident;
import com.sitewatch.model.Website;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Generates alerts. Always logs; sends email only when enabled AND a mail server is configured. */
@Slf4j
@Service
public class AlertService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean emailEnabled;
    private final String from;

    public AlertService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${sitewatch.alerts.email-enabled:false}") boolean emailEnabled,
                        @Value("${sitewatch.alerts.from:alerts@sitewatch.local}") String from) {
        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
        this.from = from;
    }

    public void siteDown(Website w, Incident incident) {
        log.warn("ALERT: {} ({}) is DOWN - {}", w.getName(), w.getUrl(), incident.getReason());
        send(w, "[SiteWatch] DOWN: " + w.getName(),
                w.getName() + " (" + w.getUrl() + ") is down.\nReason: " + incident.getReason()
                        + "\nSince: " + incident.getStartedAtText());
    }

    public void siteRecovered(Website w, Incident incident) {
        log.info("RECOVERED: {} ({}) is back UP after {}", w.getName(), w.getUrl(), incident.getDurationText());
        send(w, "[SiteWatch] RECOVERED: " + w.getName(),
                w.getName() + " (" + w.getUrl() + ") is back online.\nDowntime: " + incident.getDurationText());
    }

    private void send(Website w, String subject, String body) {
        String to = w.getOwner().getEmail();
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!emailEnabled || sender == null || to == null || to.isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
        } catch (Exception e) {
            log.error("Could not send alert email to {}: {}", to, e.getMessage());
        }
    }
}
