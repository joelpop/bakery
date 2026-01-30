package org.vaadin.bakery.app;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.vaadin.bakery.BakeryBase;

/**
 * Spring Boot application entry point.
 */
@SpringBootApplication(scanBasePackageClasses = BakeryBase.class)
@EnableVaadin("org.vaadin.bakery.ui")
@StyleSheet(Aura.STYLESHEET)
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
