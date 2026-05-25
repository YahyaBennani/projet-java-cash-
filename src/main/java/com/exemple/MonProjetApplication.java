package com.exemple;

import com.exemple.entity.User;
import com.exemple.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;

@SpringBootApplication
public class MonProjetApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonProjetApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.email}") String adminEmail,
            @Value("${admin.password}") String adminPassword,
            @Value("${admin.username}") String adminUsername) {

        return args -> {
            // ADMIN — twoFaEnabled = false, APPROVED
            if (!userRepository.existsByEmail(adminEmail)) {
                userRepository.save(User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(User.Role.ROLE_ADMIN)
                    .statut(User.Statut.APPROVED)
                    .solde(BigDecimal.ZERO)
                    .twoFaEnabled(false)
                    .build());
                System.out.println(">>> Admin créé : " + adminEmail);
            }
        };
    }
}
