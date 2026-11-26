package com.ferro.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.User;

@Repository
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository users;

    JpaUserRepository(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return users.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.findByEmail(email.value()).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return users.existsByEmail(email.value());
    }

    @Override
    public User save(User user) {
        users.save(UserJpaEntity.from(user));
        return user;
    }
}
