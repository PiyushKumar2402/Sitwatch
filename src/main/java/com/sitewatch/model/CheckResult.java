package com.sitewatch.model;

import com.sitewatch.util.Format;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One row per monitoring check (the "monitoring history"). */
@Entity
@Table(name = "check_results",
       indexes = @Index(name = "idx_check_site_time", columnList = "website_id, checked_at"))
@Getter @Setter @NoArgsConstructor
public class CheckResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "website_id")
    private Website website;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "is_up", nullable = false)
    private boolean up;

    private Integer statusCode;
    private Long responseTimeMs;
    @Column(length = 500)
    private String error;

    public String getCheckedAtText() { return Format.time(checkedAt); }
}
