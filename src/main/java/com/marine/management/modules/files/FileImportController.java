package com.marine.management.modules.files;


import com.marine.management.modules.users.domain.User;
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

    @PostMapping("/import")
    public ImportResultDto importExcel(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) throws IOException {
        return dataImportService.importFromExcel(file, user);
    }
}
