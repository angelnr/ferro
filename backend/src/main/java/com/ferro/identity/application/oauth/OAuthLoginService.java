package com.ferro.identity.application.oauth;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.identity.application.AuthResult;
import com.ferro.identity.application.EmailVerificationService;
import com.ferro.identity.application.SessionIssuer;
import com.ferro.identity.application.port.EmailSender;
import com.ferro.identity.application.port.OAuthAccountRepository;
import com.ferro.identity.application.port.UserRepository;
import com.ferro.identity.domain.DisplayName;
import com.ferro.identity.domain.Email;
import com.ferro.identity.domain.EmailTakenException;
import com.ferro.identity.domain.OAuthAccount;
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
    private final EmailVerificationService emailVerifications;
    private final EmailSender emailSender;
    private final Clock clock;

    public OAuthLoginService(
            OAuthStateService stateService,
            OAuthClientRegistry clients,
            OAuthAccountRepository accounts,
            UserRepository users,
            SessionIssuer sessions,
            EmailVerificationService emailVerifications,
            EmailSender emailSender,
            Clock clock) {
        this.stateService = stateService;
        this.clients = clients;
        this.accounts = accounts;
        this.users = users;
        this.sessions = sessions;
        this.emailVerifications = emailVerifications;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Transactional
    public OAuthCallbackResult login(OAuthProvider provider, String code, String state) {
        OAuthState parsed = stateService.parse(state);
        if (parsed.provider() != provider) {
            throw new OAuthLoginException("El state no corresponde al proveedor");
        }
        OAuthUser external = clients.clientFor(provider).fetchUser(code, parsed.codeVerifier());
        if (external == null) {
            throw new OAuthLoginException("El proveedor no devolvió identidad");
        }
        if (external.email() == null || !external.emailVerified()) {
            String onboarding =
                    stateService.createEmailOnboarding(provider, external.subject(), external.displayName());
            return new OAuthCallbackResult(null, onboarding);
        }
        User user = findLinkedUser(provider, external).orElseGet(() -> linkOrCreate(provider, external));
        return new OAuthCallbackResult(sessions.issue(user), null);
    }

    @Transactional
    public AuthResult completeEmail(OAuthProvider provider, String onboardingToken, String rawEmail) {
        OAuthOnboarding onboarding = stateService.parseEmailOnboarding(onboardingToken);
        if (onboarding.provider() != provider) {
            throw new OAuthLoginException("El token no corresponde al proveedor");
        }
        Email email = new Email(rawEmail);
        if (users.findByEmail(email).isPresent()) {
            throw new EmailTakenException();
        }
        String displayName = onboarding.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = email.value().split("@")[0];
        }
        User user = User.registerOAuth(email, new DisplayName(displayName), false, clock.instant());
        users.save(user);
        accounts.save(OAuthAccount.link(user.id(), provider, onboarding.providerSubject(), email, clock.instant()));
        emailVerifications.issueFor(user);
        emailSender.sendWelcomeEmail(user.email());
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
