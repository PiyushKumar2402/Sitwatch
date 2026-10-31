package com.sitewatch.repo;

import com.sitewatch.model.AppUser;
import com.sitewatch.model.Incident;
import com.sitewatch.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByWebsiteAndResolvedAtIsNull(Website website);

    List<Incident> findTop20ByWebsiteOrderByStartedAtDesc(Website website);

    List<Incident> findTop10ByWebsiteOwnerOrderByStartedAtDesc(AppUser owner);

    /** Incidents that overlap the window [since, now]: still open, or resolved after "since". */
    @Query("select i from Incident i where i.website = :w and (i.resolvedAt is null or i.resolvedAt >= :since)")
    List<Incident> findOverlapping(@Param("w") Website w, @Param("since") LocalDateTime since);

    @Modifying
    @Query("delete from Incident i where i.website = :w")
    void deleteByWebsite(@Param("w") Website w);
}
