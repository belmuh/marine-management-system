package com.marine.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
// @Scheduled metodların çalışması için gerekli (örn. RefreshTokenService'in
// gece 02:00 expired-token temizliği). Bu anotasyon olmadan @Scheduled
// anotasyonları sessizce yok sayılır.
@EnableScheduling
public class MarineManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarineManagementSystemApplication.class, args);
	}
}
