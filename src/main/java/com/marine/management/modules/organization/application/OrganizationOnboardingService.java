package com.marine.management.modules.organization.application;

import com.marine.management.modules.organization.application.commands.RegisterYachtCommand;
import com.marine.management.modules.organization.domain.Organization;
import com.marine.management.modules.organization.infrastructure.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrganizationOnboardingService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationOnboardingService.class);

    private final OrganizationRepository organizationRepository;

    public OrganizationOnboardingService(
            OrganizationRepository organizationRepository
    ) {
        this.organizationRepository = organizationRepository;
    }

    private Organization createOrganization(RegisterYachtCommand command) {
        Organization organization = Organization.create(
                command.yachtName(),
                command.companyName(),
                command.flagCountry(),
                command.baseCurrency()
        );

        if (command.yachtType() != null ||
                command.yachtLength() != null ||
                command.homeMarina() != null) {

            organization.updateDetails(
                    command.companyName(),
                    command.yachtType(),
                    command.yachtLength(),
                    command.homeMarina(),
                    null
            );
        }

        return organization;
    }

    public static class YachtNameAlreadyExistsException extends RuntimeException {
        public YachtNameAlreadyExistsException(String message) {
            super(message);
        }
    }

    private void validateYachtNameUnique(String yachtName) {
        if (organizationRepository.existsByYachtName(yachtName)) {
            throw new YachtNameAlreadyExistsException(
                    "Yacht name '" + yachtName + "' is already registered"
            );
        }
    }


}