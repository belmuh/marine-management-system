package com.marine.management;

import com.marine.management.modules.users.domain.User;
import com.marine.management.modules.users.infrastructure.UserRepository;
import com.marine.management.shared.kernel.security.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
public class MarineManagementSystemApplication {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {

		SpringApplication.run(MarineManagementSystemApplication.class, args);

	}

	@Bean
	public CommandLineRunner createAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				// Factory method kullanarak - RAW password ver
				User admin = User.createWithRole(
						"admin",
						"admin@marine.com",
						"admin123",  // RAW password
						Role.ADMIN
				);

				// Password'ü manuel olarak hash'le
				admin.changePassword(passwordEncoder.encode("admin123"));

				userRepository.save(admin);
				System.out.println("✅ Admin user created: admin / admin123");
			}
		};
	}

}
