package com.sitewatch.model;

import com.sitewatch.util.Format;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "websites")
@Getter @Setter @NoArgsConstructor
public class Website {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    private int intervalSeconds = 60;
    private boolean enabled = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    // Latest known state, kept here so the dashboard does not have to scan history.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SiteStatus lastStatus = SiteStatus.UNKNOWN;
    private Long lastResponseMs;
    private Integer lastStatusCode;
    private LocalDateTime lastCheckedAt;
    @Column(length = 500)
    private String lastError;

    private LocalDateTime createdAt = LocalDateTime.now();

    public String getLastCheckedAtText() { return lastCheckedAt == null ? "Not checked yet" : Format.time(lastCheckedAt); }
    public String getLastResponseText() { return lastResponseMs == null ? "—" : lastResponseMs + " ms"; }
    public String getStatusLabel() {
        if (!enabled) return "Paused";
        return switch (lastStatus) { case UP -> "Online"; case DOWN -> "Down"; default -> "Waiting"; };
    }
    public String getStatusClass() { return !enabled ? "paused" : lastStatus.name().toLowerCase(); }
}
