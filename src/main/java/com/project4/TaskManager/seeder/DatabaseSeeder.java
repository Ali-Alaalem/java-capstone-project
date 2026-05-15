package com.project4.TaskManager.seeder;


import com.project4.TaskManager.models.Role;
import com.project4.TaskManager.models.User;
import com.project4.TaskManager.repositories.RoleRepository;
import com.project4.TaskManager.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting database seeding...");
        seedRoles();
        seedAdminUser();
        log.info("Database seeding completed!");
    }


    private void seedRoles() {
        log.info("Seeding roles...");
        if (roleRepository.count() > 0) {
            log.info("Roles already exist. Skipping role seeding.");
            return;
        }
        if (!roleRepository.existsByName("ADMIN")) {
            roleRepository.save(new Role(null, "ADMIN", null));
        }
        if (!roleRepository.existsByName("USER")) {
            roleRepository.save(new Role(null, "USER", null));
        }
        log.info("Created roles: ADMIN, USER");
    }


    private void seedAdminUser() {
        log.info("Seeding admin user...");
        String adminEmail = "admin@gmail.com";
        String adminPassword = "123";

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists. Skipping admin user seeding.");
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            log.error("ADMIN role not found! Cannot create admin user.");
            return;
        }

        User admin = new User();
        admin.setFirstName("admin");
        admin.setLastName("user");
        admin.setEmail("admin@gmail.com");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(adminRole);
        admin.setIsVerified(true);
        admin.setIsDeleted(false);
        admin.setProfileImage("http://res.cloudinary.com/dqqmgoftf/image/upload/v1775576374/f4c86146-3fbb-49a2-83aa-be503a5721ce.png");
        userRepository.save(admin);

        log.info("Created default admin user: {} (password: {})", adminEmail, adminPassword);
    }
}