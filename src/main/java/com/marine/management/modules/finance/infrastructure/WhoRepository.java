package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entity.Who;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhoRepository extends JpaRepository<Who, Long> {
    Optional<Who> findByCode(String code);

    boolean existsByCode(String code);

    List<Who> findByTechnicalIs(Boolean isTechnical);

    List<Who> findBySuggestedMainCategoryId(Long mainCategoryId);
}