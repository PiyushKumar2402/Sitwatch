package com.sitewatch.repo;

import com.sitewatch.model.AppUser;
import com.sitewatch.model.Website;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebsiteRepository extends JpaRepository<Website, Long> {
    List<Website> findByOwnerOrderByNameAsc(AppUser owner);
    List<Website> findByEnabledTrue();
    Optional<Website> findByIdAndOwner(Long id, AppUser owner);
    long countByOwner(AppUser owner);
}
