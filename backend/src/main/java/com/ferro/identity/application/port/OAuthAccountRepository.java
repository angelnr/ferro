package com.ferro.identity.application.port;

import java.util.Optional;
import java.util.UUID;

import com.ferro.identity.domain.OAuthAccount;
import com.ferro.identity.domain.OAuthProvider;

public interface OAuthAccountRepository {

    Optional<OAuthAccount> findByProviderAndAccountId(OAuthProvider provider, String providerAccountId);

    Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, OAuthProvider provider);

    OAuthAccount save(OAuthAccount account);
}
