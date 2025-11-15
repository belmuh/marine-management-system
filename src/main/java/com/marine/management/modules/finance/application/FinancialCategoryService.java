package com.marine.management.modules.finance.application;

import com.marine.management.modules.finance.domain.FinancialCategory;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.shared.exceptions.CategoryNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinancialCategoryService {

    private final FinancialCategoryRepository categoryRepository;

    public FinancialCategoryService(FinancialCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public FinancialCategory create(String code, String name, String description, Integer displayOrder) {
        if (categoryRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Category code already exists: " + code);
        }

        FinancialCategory category = FinancialCategory.create(code, name, description, displayOrder);

        return categoryRepository.save(category);
    }

    @Transactional
    public FinancialCategory update(UUID id, String name, String description) {
        FinancialCategory category = getByIdOrThrow(id);
        category.updateDetails(name, description);
        return category;
    }

    @Transactional
    public FinancialCategory updateDisplayOrder(UUID id, Integer displayOrder){
        FinancialCategory category = getByIdOrThrow(id);
        category.changeDisplayOrder(displayOrder);
        return category;
    }

    @Transactional
    public FinancialCategory activate(UUID id) {
        FinancialCategory category = getByIdOrThrow(id);
        category.activate();
        return category;
    }

    @Transactional
    public FinancialCategory deactivate(UUID id) {
        FinancialCategory category = getByIdOrThrow(id);
        category.deactivate();
        return category;
    }

    @Transactional
    public void delete(UUID id) {
        FinancialCategory category = getByIdOrThrow(id);
        if (category.isActive()) {
            throw new IllegalStateException("Cannot delete active category. Deactivate first.");
        }
        categoryRepository.delete(category);
    }

    public Optional<FinancialCategory> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    public Optional<FinancialCategory> findByCode(String code) {
        return categoryRepository.findByCode(code);
    }

    public List<FinancialCategory> findAllActive() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public List<FinancialCategory> findAll() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<FinancialCategory> searchCategories(String keyword){
        return categoryRepository.search(keyword);
    }

    public boolean isCodeUnique(String code) {
        return !categoryRepository.existsByCode(code);
    }

    private FinancialCategory getByIdOrThrow(UUID id){
        return categoryRepository.findById(id).orElseThrow(() -> CategoryNotFoundException.withId(id));
    }

}
