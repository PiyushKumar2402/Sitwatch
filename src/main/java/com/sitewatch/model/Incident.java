package com.sitewatch.model;

import com.sitewatch.util.Format;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

/** A period during which a website was down. resolvedAt == null means still down. */
@Entity
@Table(name = "incidents")
@Getter @Setter @NoArgsConstructor
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "website_id")
    private Website website;

    @Column(nullable = false)
    private LocalDateTime startedAt;
    private LocalDateTime resolvedAt;
    @Column(length = 500)
    private String reason;

    public boolean isOngoing() { return resolvedAt == null; }
    public String getStartedAtText() { return Format.time(startedAt); }
    public String getResolvedAtText() { return resolvedAt == null ? "Still down" : Format.time(resolvedAt); }
    public String getDurationText() {
        return Format.duration(Duration.between(startedAt, resolvedAt == null ? LocalDateTime.now() : resolvedAt));
    }
}
