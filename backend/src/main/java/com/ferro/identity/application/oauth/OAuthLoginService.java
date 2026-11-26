package com.ferro.identity.application.oauth;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.AuthResult;
import com.ferro.identity.application.SessionIssuer;
import com.ferro.identity.application.port.OAuthAccountRepository;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.DisplayName;
import com.ferro.identity.domain.OAuthAccount;
import com.ferro.identity.domain.OAuthEmailRequiredException;
import com.ferro.identity.domain.OAuthLoginException;
import com.ferro.identity.domain.OAuthProvider;
import com.ferro.identity.domain.OAuthUser;
import com.ferro.identity.domain.User;

/** Vinculación/creación de cuentas tras validar el proveedor OAuth (RF-005/006, RN-17). */
@Service
public class OAuthLoginService {

    private final OAuthStateService stateService;
    private final OAuthClientRegistry clients;
    private final OAuthAccountRepository accounts;
    private final UserRepository users;
    private final SessionIssuer sessions;
    private final Clock clock;

    public OAuthLoginService(
            OAuthStateService stateService,
            OAuthClientRegistry clients,
            OAuthAccountRepository accounts,
            UserRepository users,
            SessionIssuer sessions,
            Clock clock) {
        this.stateService = stateService;
        this.clients = clients;
        this.accounts = accounts;
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public AuthResult login(OAuthProvider provider, String code, String state) {
        OAuthState parsed = stateService.parse(state);
        if (parsed.provider() != provider) {
            throw new OAuthLoginException("El state no corresponde al proveedor");
        }
        OAuthUser external = clients.clientFor(provider).fetchUser(code, parsed.codeVerifier());
        if (external == null) {
            throw new OAuthLoginException("El proveedor no devolvió identidad");
        }
        if (external.email() == null || !external.emailVerified()) {
            throw new OAuthEmailRequiredException();
        }
        User user = findLinkedUser(provider, external).orElseGet(() -> linkOrCreate(provider, external));
        return sessions.issue(user);
    }

    private Optional<User> findLinkedUser(OAuthProvider provider, OAuthUser external) {
        return accounts.findByProviderAndAccountId(provider, external.subject())
                .flatMap(account -> users.findById(account.userId()));
    }

    private User linkOrCreate(OAuthProvider provider, OAuthUser external) {
        User user = users.findByEmail(external.email()).orElseGet(() -> createUser(external));
        if (!user.isEmailVerified()) {
            user.verifyEmail(clock.instant());
            users.save(user);
        }
        Optional<OAuthAccount> existing = accounts.findByUserIdAndProvider(user.id(), provider);
        if (existing.isEmpty()) {
            accounts.save(
                    OAuthAccount.link(user.id(), provider, external.subject(), external.email(), clock.instant()));
        }
        return user;
    }

    private User createUser(OAuthUser external) {
        String displayName = external.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = external.email().value().split("@")[0];
        }
        User user = User.registerOAuth(
                external.email(), new DisplayName(displayName), external.emailVerified(), clock.instant());
        return users.save(user);
    }
}
