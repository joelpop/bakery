package org.vaadin.bakery.jpaservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpaclient.repository.UserRepository;
import org.vaadin.bakery.jpaservice.mapper.UserMapper;
import org.vaadin.bakery.service.CurrentUserService;
import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.type.UserRole;

import java.util.Optional;

/**
 * Implementation of CurrentUserService that accesses the authenticated user from Spring Security.
 */
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public CurrentUserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Optional<String> getCurrentUserEmail() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .filter(name -> !"anonymousUser".equals(name));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetail> getCurrentUser() {
        return getCurrentUserEmail()
                .flatMap(userRepository::findByEmailIgnoreCase)
                .map(userMapper::toDetail);
    }

    @Override
    public boolean hasRole(UserRole role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
    }

    @Override
    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    @Override
    public boolean isBaker() {
        return hasRole(UserRole.BAKER);
    }

    @Override
    public boolean isBarista() {
        return hasRole(UserRole.BARISTA);
    }
}
