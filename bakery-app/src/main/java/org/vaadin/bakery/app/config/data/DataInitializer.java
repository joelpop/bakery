package org.vaadin.bakery.app.config.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.vaadin.bakery.service.UserService;
import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.type.UserRole;

/**
 * Initializes demo data on application startup.
 * Only runs in development (default) profile.
 */
@Component
@Profile("!production")
public class DataInitializer implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);

    private final UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (!userService.emailExists("admin@cafe-sunshine.com")) {
            createDefaultUsers();
        } else {
            LOG.info("Demo data already exists, skipping initialization");
        }
    }

    private void createDefaultUsers() {
        LOG.info("Creating demo users...");

        // Admin user
        var admin = new UserDetail();
        admin.setEmail("admin@cafe-sunshine.com");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setPassword("admin123");
        admin.setRole(UserRole.ADMIN);
        userService.create(admin);
        LOG.info("Created admin user: admin@cafe-sunshine.com / admin123");

        // Baker user
        var baker = new UserDetail();
        baker.setEmail("baker@cafe-sunshine.com");
        baker.setFirstName("Baker");
        baker.setLastName("Smith");
        baker.setPassword("baker123");
        baker.setRole(UserRole.BAKER);
        userService.create(baker);
        LOG.info("Created baker user: baker@cafe-sunshine.com / baker123");

        // Barista user
        var barista = new UserDetail();
        barista.setEmail("barista@cafe-sunshine.com");
        barista.setFirstName("Barista");
        barista.setLastName("Jones");
        barista.setPassword("barista123");
        barista.setRole(UserRole.BARISTA);
        userService.create(barista);
        LOG.info("Created barista user: barista@cafe-sunshine.com / barista123");

        LOG.info("Demo data initialization complete");
    }
}
