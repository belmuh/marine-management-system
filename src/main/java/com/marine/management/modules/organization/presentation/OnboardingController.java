package com.marine.management.modules.organization.presentation;

import com.marine.management.modules.finance.application.TenantReferenceDataInitializer;
import com.marine.management.modules.finance.domain.entities.MainCategory;
import com.marine.management.modules.finance.domain.entities.Who;
import com.marine.management.modules.finance.infrastructure.MainCategoryRepository;
import com.marine.management.modules.finance.infrastructure.WhoRepository;
import com.marine.management.modules.organization.application.OrganizationOnboardingService;
import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.domain.YachtType;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import com.marine.management.modules.organization.presentation.dto.SetupRequest;
import com.marine.management.modules.organization.presentation.dto.SetupResponse;
import com.marine.management.modules.users.domain.User;
import com.marine.management.shared.multitenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public endpoint for yacht registration wizard.
 * No authentication required — this creates the first user.
 */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private static final Logger logger = LoggerFactory.getLogger(OnboardingController.class);

    private final OrganizationOnboardingService onboardingService;
    private final MainCategoryRepository mainCategoryRepository;
    private final WhoRepository whoRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantReferenceDataInitializer tenantReferenceDataInitializer;

    public OnboardingController(
            OrganizationOnboardingService onboardingService,
            MainCategoryRepository mainCategoryRepository,
            WhoRepository whoRepository,
            OrganizationRepository organizationRepository,
            TenantReferenceDataInitializer tenantReferenceDataInitializer
    ) {
        this.onboardingService = onboardingService;
        this.mainCategoryRepository = mainCategoryRepository;
        this.whoRepository = whoRepository;
        this.organizationRepository = organizationRepository;
        this.tenantReferenceDataInitializer = tenantReferenceDataInitializer;
    }

    /**
     * Register a new yacht organization with admin user.
     * Receives all wizard step data in a single call.
     */
    @PostMapping("/register")
    public ResponseEntity<OnboardingResponse> register(
            @Valid @RequestBody OnboardingRequest request
    ) {
        logger.info("Received onboarding request for yacht: {}", request.yachtName());

        RegisterYachtCommand command = new RegisterYachtCommand(
                request.yachtName(),
                request.yachtType(),
                request.yachtLength(),
                request.flagCountry(),
                request.homeMarina(),
                request.companyName(),
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.baseCurrency(),
                request.timezone(),
                request.financialYearStartMonth(),
                request.approvalLimit(),
                request.managerApprovalEnabled(),
                request.selectedMainCategoryIds(),
                request.selectedWhoIds()
        );

        OnboardingResult result = onboardingService.registerYacht(command);

        logger.info("Onboarding completed for yacht: {} (tenant: {})",
                result.yachtName(), result.organizationId());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new OnboardingResponse(
                        result.organizationId(),
                        result.userId(),
                        result.yachtName(),
                        result.email(),
                        "Yacht registered successfully."
                )
        );
    }

    /**
     * Complete the onboarding setup after registration and email verification.
     * Requires authentication (user must be logged in).
     * Updates organization with yacht details, financial settings, and initializes categories.
     */
    @PostMapping("/setup")
    public ResponseEntity<SetupResponse> completeSetup(
            @Valid @RequestBody SetupRequest request,
            @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Organization organization = user.getOrganization();

        if (organization.isOnboardingCompleted()) {
            return ResponseEntity.badRequest().body(new SetupResponse(
                    organization.getOrganizationId(),
                    organization.getYachtName(),
                    "Onboarding already completed.",
                    true
            ));
        }

        logger.info("Completing setup for organization: {} (id: {})",
                organization.getYachtName(), organization.getOrganizationId());

        // Update organization with full setup data
        organization.completeSetup(
                request.companyName(),
                request.flagCountry(),
                request.baseCurrency(),
                request.yachtType(),
                request.yachtLength(),
                request.homeMarina(),
                request.timezone(),
                request.financialYearStartMonth(),
                Boolean.TRUE.equals(request.managerApprovalEnabled()),
                request.approvalLimit()
        );

        organizationRepository.save(organization);

        // Initialize tenant reference data
        Long tenantId = organization.getOrganizationId();
        TenantContext.setCurrentTenantId(tenantId);
        try {
            tenantReferenceDataInitializer.initializeTenantReferenceData(
                    request.selectedMainCategoryIds(),
                    request.selectedWhoIds()
            );
        } finally {
            TenantContext.clear();
        }

        logger.info("Setup completed for organization: {} (id: {})",
                organization.getYachtName(), tenantId);

        return ResponseEntity.ok(SetupResponse.success(tenantId, organization.getYachtName()));
    }

    /**
     * Public endpoint: returns global reference data (main categories & WHO list).
     * Used by Step 4 of the wizard to preview default categories.
     */
    @GetMapping("/reference-data")
    public ResponseEntity<ReferenceDataPreview> getReferenceData() {
        List<MainCategoryPreview> categories = mainCategoryRepository.findAll().stream()
                .map(mc -> new MainCategoryPreview(
                        mc.getId(), mc.getCode(), mc.getNameTr(), mc.getNameEn(),
                        mc.isTechnical(), mc.getDisplayOrder()
                ))
                .sorted((a, b) -> Integer.compare(
                        a.displayOrder() != null ? a.displayOrder() : 999,
                        b.displayOrder() != null ? b.displayOrder() : 999))
                .toList();

        List<WhoPreview> whoList = whoRepository.findAll().stream()
                .map(w -> new WhoPreview(
                        w.getId(), w.getCode(), w.getNameTr(), w.getNameEn(),
                        w.isTechnical(), w.getDisplayOrder()
                ))
                .sorted((a, b) -> Integer.compare(
                        a.displayOrder() != null ? a.displayOrder() : 999,
                        b.displayOrder() != null ? b.displayOrder() : 999))
                .toList();

        return ResponseEntity.ok(new ReferenceDataPreview(categories, whoList));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Onboarding API is accessible");
    }

    // ── Request / Response records (keep in same file, simple DTOs) ──

    public record OnboardingRequest(
            // Step 1 — Yacht
            @NotBlank(message = "Yacht name is required")
            @Size(min = 2, max = 100)
            String yachtName,

            @NotNull(message = "Yacht type is required")
            YachtType yachtType,

            @Min(value = 5, message = "Yacht length must be at least 5 meters")
            @Max(value = 200, message = "Yacht length cannot exceed 200 meters")
            Integer yachtLength,

            @NotBlank(message = "Flag country is required")
            @Size(min = 2, max = 2)
            String flagCountry,

            String homeMarina,

            // Step 2 — Company & Admin
            String companyName,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 100)
            String password,

            @NotBlank(message = "First name is required")
            @Size(min = 2, max = 50)
            String firstName,

            @NotBlank(message = "Last name is required")
            @Size(min = 2, max = 50)
            String lastName,

            String phoneNumber,

            // Step 3 — Financial
            @NotBlank(message = "Currency is required")
            @Size(min = 3, max = 3)
            String baseCurrency,

            String timezone,

            @Min(1) @Max(12)
            Integer financialYearStartMonth,

            BigDecimal approvalLimit,
            Boolean managerApprovalEnabled,

            // Step 4 — Category & WHO Selections (null = enable all)
            java.util.Set<Long> selectedMainCategoryIds,
            java.util.Set<Long> selectedWhoIds
    ) {}

    public record OnboardingResponse(
            Long organizationId,
            java.util.UUID adminUserId,
            String yachtName,
            String adminEmail,
            String message
    ) {}

    public record MainCategoryPreview(
            Long id, String code, String nameTr, String nameEn,
            Boolean technical, Integer displayOrder
    ) {}

    public record WhoPreview(
            Long id, String code, String nameTr, String nameEn,
            boolean technical, Integer displayOrder
    ) {}

    public record ReferenceDataPreview(
            List<MainCategoryPreview> mainCategories,
            List<WhoPreview> whoList
    ) {}
}
