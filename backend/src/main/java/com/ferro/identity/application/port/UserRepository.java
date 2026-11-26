package com.ferro.identity.application.port;

import java.util.Optional;
import java.util.UUID;

import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.User;

/** Puerto de salida del agregado User. */
public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    User save(User user);

    void delete(User user);
}
