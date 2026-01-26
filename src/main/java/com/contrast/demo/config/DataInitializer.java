package com.contrast.demo.config;

import com.contrast.demo.model.User;
import com.contrast.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            // Initialize with some test data
            userRepository.save(new User("admin", "admin123", "admin@example.com", "ADMIN"));
            userRepository.save(new User("user", "password", "user@example.com", "USER"));
            userRepository.save(new User("test", "test123", "test@example.com", "USER"));
        };
    }
}
