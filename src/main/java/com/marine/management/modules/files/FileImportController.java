package com.marine.management.modules.files;

import com.marine.management.modules.users.domain.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
public class FileImportController {

    private final DataImportService dataImportService;

    public FileImportController(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    /**
     * Imports historical financial entries from an Excel file.
     * Restricted to CAPTAIN+ (bulk data load + auto category creation is high-impact).
     */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('CATEGORY_MANAGE')")
    public ImportResultDto importExcel(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) throws IOException {
        return dataImportService.importFromExcel(file, user);
    }
}
