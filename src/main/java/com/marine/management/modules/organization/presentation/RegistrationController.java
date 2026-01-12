package com.marine.management.modules.organization.presentation;

import com.marine.management.modules.organization.application.OrganizationOnboardingService;
import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.application.commands.OnboardingResult;
import com.marine.management.modules.organization.presentation.dto.RegistrationRequest;
import com.marine.management.modules.organization.presentation.dto.RegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final OrganizationOnboardingService onboardingService;

    public RegistrationController(OrganizationOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request
    ) {
        try {
            var command = new RegisterYachtCommand(
                    request.yachtName(),
                    request.companyName(),
                    request.flagCountry(),
                    request.baseCurrency(),
                    request.yachtType(),
                    request.yachtLength(),
                    request.homeMarina(),
                    request.username(),
                    request.email(),
                    request.password(),
                    request.firstName(),
                    request.lastName()
            );

            OnboardingResult result = onboardingService.registerNewYacht(command);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new RegistrationResponse(
                            true,
                            "Registration successful! Welcome to Marine Management System.",
                            result.yachtName(),
                            result.email()
                    ));

        } catch (OrganizationOnboardingService.YachtNameAlreadyExistsException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new RegistrationResponse(false, e.getMessage(), null, null));

        } catch (OrganizationOnboardingService.EmailAlreadyExistsException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new RegistrationResponse(false, e.getMessage(), null, null));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new RegistrationResponse(false, e.getMessage(), null, null));
        }
    }
}
