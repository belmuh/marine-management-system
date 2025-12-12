package com.marine.management.modules.finance.infrastructure;

import com.marine.management.modules.finance.domain.entity.MainCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MainCategoryRepository extends JpaRepository<MainCategory, Long> {

    Optional<MainCategory> findByCode(String code);

    boolean existsByCode(String code);

    List<MainCategory> findByActiveTrue();

    List<MainCategory> findByTechnical(Boolean technical);

    List<MainCategory> findByTechnicalAndActive(Boolean technical, Boolean active);
}