package com.vantrack.config;

import com.vantrack.users.User;
import com.vantrack.users.UserRepository;
import com.vantrack.users.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeederConfig {

    @Bean
    public CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@vantrack.com";

            if (userRepository.findByEmail(adminEmail).isEmpty()) {

                User admin = new User();
                admin.setName("Administrador do Sistema");
                admin.setEmail(adminEmail);

                admin.setPassword(passwordEncoder.encode("admin123"));

                admin.setRole(UserRole.ADMIN);

                userRepository.save(admin);

                System.out.println("✅ Usuário ADMIN padrão criado com sucesso!");
                System.out.println("Email: " + adminEmail);
                System.out.println("Senha: admin123");
            } else {
                System.out.println("ℹ️ Usuário ADMIN já existe no banco de dados.");
            }
        };
    }
}