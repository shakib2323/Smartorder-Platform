package com.shakib.smartorder.auth.config;


import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.shakib.smartorder.auth.entity.User;
import com.shakib.smartorder.auth.enums.Role;
import com.shakib.smartorder.auth.repository.UserRepository;

// Creates the first admin account so there is someone who can log in right after a fresh install. 
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${app.admin.name:Administrator}") String name,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) 
    {
        if (email.isBlank() || password.isBlank()) 
        {
            log.warn("ADMIN_PASSWORD is not set - no admin account was created. Set it to seed the first admin.");
            return;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalized)) {
            return;
        }
        userRepository.save(new User(name, normalized, passwordEncoder.encode(password), Role.ADMIN));
        log.warn("Created default admin account {}. Change its password before using this anywhere real.", normalized);
    }
}
