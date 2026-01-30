package org.vaadin.bakery.jpaservice.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpaclient.repository.UserRepository;

import java.util.List;

/**
 * Spring Security UserDetailsService implementation that loads users from the database.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var userEntity = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        var authority = new SimpleGrantedAuthority("ROLE_" + userEntity.getRole().name());

        return new User(
                userEntity.getEmail(),
                userEntity.getPasswordHash(),
                true,
                true,
                true,
                true,
                List.of(authority)
        );
    }
}
