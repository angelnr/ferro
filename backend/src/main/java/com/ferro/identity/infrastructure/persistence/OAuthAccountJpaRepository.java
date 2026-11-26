package com.ferro.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ferro.identity.domain.OAuthProvider;

interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccountJpaEntity, UUID> {

    Optional<OAuthAccountJpaEntity> findByProviderAndProviderAccountId(
            OAuthProvider provider, String providerAccountId);

    Optional<OAuthAccountJpaEntity> findByUserIdAndProvider(UUID userId, OAuthProvider provider);
}
