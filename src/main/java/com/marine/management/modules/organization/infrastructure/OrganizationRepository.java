package com.marine.management.modules.organization.infrastructure;

import com.marine.management.modules.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByYachtName(String yachtName);

    boolean existsByYachtName(String yachtName);

    // ✅ NEW: Find all active organizations (for scheduled tasks)
    List<Organization> findAllByActiveTrue();
}