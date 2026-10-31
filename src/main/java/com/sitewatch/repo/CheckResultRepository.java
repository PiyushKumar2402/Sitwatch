package com.sitewatch.repo;

import com.sitewatch.model.CheckResult;
import com.sitewatch.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    List<CheckResult> findTop30ByWebsiteOrderByCheckedAtDesc(Website website);

    List<CheckResult> findTop60ByWebsiteOrderByCheckedAtDesc(Website website);

    List<CheckResult> findByWebsiteAndCheckedAtGreaterThanEqualOrderByCheckedAtAsc(Website website, LocalDateTime since);

    @Query("select count(c) from CheckResult c where c.website = :w and c.checkedAt >= :since")
    long countSince(@Param("w") Website w, @Param("since") LocalDateTime since);

    @Query("select count(c) from CheckResult c where c.website = :w and c.up = true and c.checkedAt >= :since")
    long countUpSince(@Param("w") Website w, @Param("since") LocalDateTime since);

    @Query("select avg(c.responseTimeMs) from CheckResult c where c.website = :w and c.up = true and c.checkedAt >= :since")
    Double avgResponseSince(@Param("w") Website w, @Param("since") LocalDateTime since);

    @Modifying
    @Query("delete from CheckResult c where c.checkedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("delete from CheckResult c where c.website = :w")
    void deleteByWebsite(@Param("w") Website w);
}
