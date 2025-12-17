package com.marine.management.modules.finance.infrastructure.loader;

import com.marine.management.modules.finance.domain.entity.MainCategory;
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
                createWho("CREW", "Personel", "Crew", false, DataLoaderSharedData.getMainCategoryId("CREW_EXPENSES")),
                createWho("CAPTAIN", "Kaptan", "Captain", false, DataLoaderSharedData.getMainCategoryId("CREW_EXPENSES")),
                createWho("GUEST", "Misafir", "Guest", false, DataLoaderSharedData.getMainCategoryId("GUEST")),
                createWho("OWNER", "Tekne Sahibi", "Owner", false, DataLoaderSharedData.getMainCategoryId("OWNER")),
                createWho("MARINA", "Marina", "Marina", false, DataLoaderSharedData.getMainCategoryId("OPERATIONAL")),
                createWho("OFFICE", "Ofis", "Office", false, DataLoaderSharedData.getMainCategoryId("OPERATIONAL")),

                // TECHNICAL
                createWho("YACHT", "Yat", "Yacht", true, null),
                createWho("MAIN_ENGINE", "Ana Makine", "Main Engine", true, DataLoaderSharedData.getMainCategoryId("MAINTENANCE")),
                createWho("GENERATOR", "Jeneratör", "Generator", true, DataLoaderSharedData.getMainCategoryId("MAINTENANCE")),
                createWho("TENDER", "Tender", "Tender", true, DataLoaderSharedData.getMainCategoryId("MAINTENANCE")),
                createWho("JETSKI", "Jetski", "Jetski", true, DataLoaderSharedData.getMainCategoryId("MAINTENANCE")),
                createWho("WATERMAKER", "Su Yapıcı", "Watermaker", true, DataLoaderSharedData.getMainCategoryId("MAINTENANCE")),
                createWho("AC_SYSTEM", "Klima Sistemi", "AC System", true, DataLoaderSharedData.getMainCategoryId("MAINTENANCE")),
                createWho("ELECTRICAL", "Elektrik", "Electrical", true, DataLoaderSharedData.getMainCategoryId("MAINTENANCE")),
                createWho("PLUMBING", "Tesisat", "Plumbing", true, DataLoaderSharedData.getMainCategoryId("MAINTENANCE"))
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