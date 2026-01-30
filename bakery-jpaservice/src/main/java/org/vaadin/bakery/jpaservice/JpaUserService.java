package org.vaadin.bakery.jpaservice;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpamodel.entity.UserEntity;
import org.vaadin.bakery.jpaclient.repository.UserRepository;
import org.vaadin.bakery.jpaservice.mapper.UserMapper;
import org.vaadin.bakery.service.UserService;
import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.data.UserSummary;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of the user service.
 */
@Service
@Transactional
public class JpaUserService implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public JpaUserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> list() {
        return userMapper.toSummaryList(userRepository.findAllProjectedBy());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> search(String query) {
        if (query == null || query.isBlank()) {
            return list();
        }
        var lowerQuery = query.toLowerCase();
        return list().stream()
                .filter(u -> u.getEmail().toLowerCase().contains(lowerQuery) ||
                        u.getFirstName().toLowerCase().contains(lowerQuery) ||
                        u.getLastName().toLowerCase().contains(lowerQuery))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetail> get(Long id) {
        return userRepository.findById(id).map(userMapper::toDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDetail> getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email).map(userMapper::toDetail);
    }

    @Override
    public UserDetail create(UserDetail user) {
        var entity = userMapper.toNewEntity(user);
        entity.setPasswordHash(passwordEncoder.encode(user.getPassword()));
        var saved = userRepository.save(entity);
        return userMapper.toDetail(saved);
    }

    @Override
    public UserDetail update(Long id, UserDetail user) {
        var entity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        userMapper.toEntity(user, entity);
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(user.getPassword()));
        }
        return userMapper.toDetail(entity);
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void changePassword(Long id, String newPassword) {
        var entity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        entity.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExistsForOtherUser(String email, Long userId) {
        return userRepository.existsByEmailAndIdNot(email, userId);
    }
}
