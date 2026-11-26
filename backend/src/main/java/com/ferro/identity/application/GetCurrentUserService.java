package com.ferro.identity.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.InvalidCredentialsException;
import com.ferro.identity.domain.User;

/** Lectura del usuario autenticado para GET /auth/me. */
@Service
public class GetCurrentUserService {

    private final UserRepository users;

    public GetCurrentUserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User byId(UUID id) {
        return users.findById(id).orElseThrow(InvalidCredentialsException::new);
    }
}
