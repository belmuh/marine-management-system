package com.marine.management.modules.auth.presentation;

import com.marine.management.modules.auth.application.RegistrationService;
import com.marine.management.modules.auth.presentation.dto.RegisterOrganizationRequest;
import com.marine.management.modules.auth.presentation.dto.RegisterOrganizationResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterOrganizationResponse> registerOrganization(
            @Valid @RequestBody RegisterOrganizationRequest request
    ) {
        logger.info("Received registration request for organization: {}", request.organizationName());

        RegisterOrganizationResponse response = registrationService.registerNewOrganization(request);

        logger.info("Successfully registered organization: {} with ID: {}",
                response.organizationName(), response.organizationId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Public API is accessible");
    }
}