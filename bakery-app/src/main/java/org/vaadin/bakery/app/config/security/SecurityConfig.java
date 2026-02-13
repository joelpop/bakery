package org.vaadin.bakery.app.config.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.vaadin.bakery.ui.view.login.LoginView;

/**
 * Spring Security configuration for the Bakery application.
 * Features:
 * - Vaadin security integration
 * - Session management with concurrent session control
 * - BCrypt password encoding
 *
 * Note: WebAuthn passkey authentication is prepared in the UI but requires
 * additional configuration with webauthn4j-core dependency when ready.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /** Configures the security filter chain with Vaadin integration, login view, and session management. */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginView.class);
        })
        .sessionManagement(session -> {
            session.maximumSessions(5);
        })
        .build();
    }

    /** Provides a BCrypt password encoder for hashing user passwords. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
