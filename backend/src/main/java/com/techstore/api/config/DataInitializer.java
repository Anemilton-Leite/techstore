package com.techstore.api.config;

import com.techstore.api.entity.Role;
import com.techstore.api.entity.User;
import com.techstore.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {
        return args -> {
            String normalizedEmail = adminEmail.trim().toLowerCase();
            if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                userRepository.save(new User(
                        "TechStore Admin",
                        normalizedEmail,
                        passwordEncoder.encode(adminPassword),
                        Role.ROLE_ADMIN));
            }
        };
    }
}
