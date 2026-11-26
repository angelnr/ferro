package com.ferro.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.ferro.identity.application.port.OAuthAccountRepository;
import com.ferro.identity.domain.OAuthAccount;
import com.ferro.identity.domain.OAuthProvider;

@Repository
class JpaOAuthAccountRepository implements OAuthAccountRepository {

    private final OAuthAccountJpaRepository accounts;

    JpaOAuthAccountRepository(OAuthAccountJpaRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<OAuthAccount> findByProviderAndAccountId(OAuthProvider provider, String providerAccountId) {
        return accounts.findByProviderAndProviderAccountId(provider, providerAccountId)
                .map(OAuthAccountJpaEntity::toDomain);
    }

    @Override
    public Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, OAuthProvider provider) {
        return accounts.findByUserIdAndProvider(userId, provider).map(OAuthAccountJpaEntity::toDomain);
    }

    @Override
    public OAuthAccount save(OAuthAccount account) {
        accounts.save(OAuthAccountJpaEntity.from(account));
        return account;
    }
}
