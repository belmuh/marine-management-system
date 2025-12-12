package com.marine.management.modules.finance.presentation;

import com.marine.management.modules.finance.application.FinancialCategoryService;
import com.marine.management.modules.finance.domain.enums.RecordType;
import com.marine.management.modules.finance.domain.entity.FinancialCategory;
import com.marine.management.modules.finance.infrastructure.FinancialCategoryRepository;
import com.marine.management.modules.finance.presentation.dto.CategoryRequestDto;
import com.marine.management.modules.finance.presentation.dto.CategoryResponseDto;
import com.marine.management.modules.finance.presentation.dto.CategoryWithUsageDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance/categories")
public class FinancialCategoryController {

    private final FinancialCategoryService categoryService;

    public FinancialCategoryController(FinancialCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> create(@Valid @RequestBody CategoryRequestDto request) {
        FinancialCategory category = categoryService.create(
                request.code(),
                request.name(),
                request.categoryType(),
                request.description(),
                request.displayOrder(),
                request.isTechnical()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CategoryResponseDto.from(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getById(@PathVariable UUID id) {
        return categoryService.findById(id)
                .map(CategoryResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<CategoryResponseDto> getByCode(@PathVariable String code) {
        return categoryService.findByCode(code)
                .map(CategoryResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        List<FinancialCategory> categories = activeOnly
                ? categoryService.findAllActive()
                : categoryService.findAll();

        return ResponseEntity.ok(
                categories.stream()
                        .map(CategoryResponseDto::from)
                        .toList()
        );
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<CategoryWithUsageDto>> getByTypeWithUsage(
            @RequestParam RecordType entryType,
             @RequestParam LocalDate oneYearAgo
    ) {
        List<FinancialCategoryRepository.CategoryWithUsageCount> categoriesWithUsage =
                categoryService.findByTypeWithUsageCount(entryType, oneYearAgo);

        return ResponseEntity.ok(
                categoriesWithUsage.stream()
                        .map(CategoryWithUsageDto::from)
                        .toList()
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<CategoryResponseDto>> search(
            @RequestParam String keyword
    ) {
        List<FinancialCategory> categories = categoryService.searchCategories(keyword);

        return ResponseEntity.ok(
                categories.stream()
                        .map(CategoryResponseDto::from)
                        .toList()
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequestDto request
    ) {
        FinancialCategory category = categoryService.update(
                id,
                request.name(),
                request.description(),
                request.categoryType(),
                request.isTechnical()
        );

        return ResponseEntity.ok(CategoryResponseDto.from(category));
    }

    @PatchMapping("/{id}/display-order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> updateDisplayOrder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDisplayOrderRequest request
    ) {
        FinancialCategory category = categoryService.updateDisplayOrder(
                id,
                request.displayOrder()
        );

        return ResponseEntity.ok(CategoryResponseDto.from(category));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> activate(@PathVariable UUID id) {
        FinancialCategory category = categoryService.activate(id);
        return ResponseEntity.ok(CategoryResponseDto.from(category));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDto> deactivate(@PathVariable UUID id) {
        FinancialCategory category = categoryService.deactivate(id);
        return ResponseEntity.ok(CategoryResponseDto.from(category));
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // VALIDATION
    @GetMapping("/validate/code")
    public ResponseEntity<ValidationResponse> validateCode(
            @RequestParam String code
    ) {
        boolean isUnique = categoryService.isCodeUnique(code);
        return ResponseEntity.ok(new ValidationResponse(isUnique,
                isUnique ? "Code is available" : "Code already exists"));
    }


    public record ValidationResponse(
            boolean valid,
            String message
    ) {}

    public record UpdateDisplayOrderRequest(
            Integer displayOrder
    ) {}
}
