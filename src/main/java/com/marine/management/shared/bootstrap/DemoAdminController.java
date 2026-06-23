package com.marine.management.shared.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demo data yönetim endpoint'leri — sadece SUPER_ADMIN erişebilir.
 *
 * POST /api/admin/demo/reset
 *   Demo tenant'ı siler ve taze demo data oluşturur.
 *   Diğer tenant'lara dokunmaz.
 */
@RestController
@RequestMapping("/api/admin/demo")
@PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
public class DemoAdminController {

    private static final Logger log = LoggerFactory.getLogger(DemoAdminController.class);

    private final DemoDataService demoDataService;

    public DemoAdminController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetDemoData() {
        log.info("🔄 Demo reset endpoint çağrıldı");
        demoDataService.reset();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Demo data sıfırlandı. Kullanıcılar: Demo123!"
        ));
    }
}
