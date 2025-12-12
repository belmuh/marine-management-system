package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.entity.MainCategory;
import com.marine.management.modules.finance.domain.entity.Who;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.WhoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final MainCategoryRepository mainCategoryRepository;
    private final WhoRepository whoRepository;

    public ReferenceDataService(
            MainCategoryRepository mainCategoryRepository,
            WhoRepository whoRepository
    ) {
        this.mainCategoryRepository = mainCategoryRepository;
        this.whoRepository = whoRepository;
    }

    // ============================================
    // MAIN CATEGORY QUERIES
    // ============================================

    public List<MainCategory> getAllMainCategories() {
        return mainCategoryRepository.findAll();
    }

    public List<MainCategory> getActiveMainCategories() {
        return mainCategoryRepository.findByActiveTrue();
    }

    public List<MainCategory> getMainCategoriesByType(boolean technical) {
        return mainCategoryRepository.findByTechnical(technical);
    }

    public Optional<MainCategory> getMainCategoryById(Long id) {
        return mainCategoryRepository.findById(id);
    }

    public Optional<MainCategory> getMainCategoryByCode(String code) {
        return mainCategoryRepository.findByCode(code);
    }

    // ============================================
    // WHO QUERIES
    // ============================================

    public List<Who> getAllWho() {
        return whoRepository.findAll();
    }

    public List<Who> getActiveWho() {
        return whoRepository.findByActiveTrue();
    }

    public List<Who> getWhoByType(boolean technical) {
        return whoRepository.findByTechnicalIs(technical);
    }

    public List<Who> getWhoBySuggestedMainCategory(Long mainCategoryId) {
        return whoRepository.findBySuggestedMainCategoryId(mainCategoryId);
    }

    public Optional<Who> getWhoById(Long id) {
        return whoRepository.findById(id);
    }

    public Optional<Who> getWhoByCode(String code) {
        return whoRepository.findByCode(code);
    }
}