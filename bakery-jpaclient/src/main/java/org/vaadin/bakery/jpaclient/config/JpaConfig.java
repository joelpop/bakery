package org.vaadin.bakery.jpaclient.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for entity scanning and repository registration.
 */
@Configuration
@EntityScan(basePackages = "org.vaadin.bakery.jpamodel")
@EnableJpaRepositories(basePackages = "org.vaadin.bakery.jpaclient")
public class JpaConfig {
}
