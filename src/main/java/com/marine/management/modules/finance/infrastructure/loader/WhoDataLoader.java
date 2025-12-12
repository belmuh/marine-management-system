package com.marine.management.modules.finance.infrastructure.loader;

import com.marine.management.modules.finance.domain.entity.Who;
import com.marine.management.modules.finance.infrastructure.WhoRepository;
import com.marine.management.shared.config.GlobalExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Order(2) // MainCategory'den sonra çalışsın
public class WhoDataLoader implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final WhoRepository whoRepository;

    public WhoDataLoader(WhoRepository whoRepository) {
        this.whoRepository = whoRepository;
    }

    @Override
    public void run(String... args) {
        if (whoRepository.count() > 0) {
            logger.info("Who list already exists, skipping seed data");
            return;
        }

        logger.info("Loading who list seed data...");

        List<Who> whoList = Arrays.asList(
                // NON_TECHNICAL
                createWho("CREW", "Mürettebat", "Crew", false, null),
                createWho("CAPTAIN", "Kaptan", "Captain", false, null),
                createWho("GUEST", "Misafir", "Guest", false, null),
                createWho("OWNER", "Mal Sahibi", "Owner", false, null),
                createWho("MARINA", "Marina", "Marina", false, null),
                createWho("OFFICE", "Ofis", "Office", false, null),

                // TECHNICAL
                createWho("YACHT", "Yat", "Yacht", true, null),
                createWho("MAIN_ENGINE", "Ana Makine", "Main Engine", true, null),
                createWho("GENERATOR", "Jeneratör", "Generator", true, null),
                createWho("TENDER", "Tender", "Tender", true, null),
                createWho("JETSKI", "Jetski", "Jetski", true, null),
                createWho("WATERMAKER", "Su Yapıcı", "Watermaker", true, null),
                createWho("AC_SYSTEM", "Klima Sistemi", "AC System", true, null),
                createWho("ELECTRICAL", "Elektrik", "Electrical", true, null),
                createWho("PLUMBING", "Tesisat", "Plumbing", true, null)
        );

        whoRepository.saveAll(whoList);
        logger.info("Who list seed data loaded successfully: {} entries", whoList.size());
    }

    private Who createWho(String code, String nameTr, String nameEn,
                          boolean isTechnical, Long suggestedMainCategoryId) {
        Who who = new Who();
        who.setCode(code);
        who.setNameTr(nameTr);
        who.setNameEn(nameEn);
        who.setTechnical(isTechnical);
        who.setSuggestedMainCategoryId(suggestedMainCategoryId);
        who.setActive(true);
        return who;
    }
}