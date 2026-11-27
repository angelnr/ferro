package com.ferro.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OAuthDomainTest {

    @Test
    void maps_provider_paths_and_codes() {
        assertThat(OAuthProvider.fromPath("google")).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(OAuthProvider.fromPath("github")).isEqualTo(OAuthProvider.GITHUB);
        assertThat(AppLocale.fromCode("en")).isEqualTo(AppLocale.EN);
        assertThat(AppLocale.fromCode("es")).isEqualTo(AppLocale.ES);
        assertThat(AppLocale.ES.code()).isEqualTo("es");
    }

    @Test
    void oauth_account_exposes_its_state() {
        OAuthAccount account = OAuthAccount.link(
                java.util.UUID.randomUUID(),
                OAuthProvider.GOOGLE,
                "provider-id",
                new Email("u@ferro.dev"),
                Instant.now());
        assertThat(account.id()).isNotNull();
        assertThat(account.userId()).isNotNull();
        assertThat(account.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(account.providerAccountId()).isEqualTo("provider-id");
        assertThat(account.emailAtLink()).isNotNull();
        assertThat(account.createdAt()).isNotNull();
        assertThat(OAuthAccount.rehydrate(
                                account.id(),
                                account.userId(),
                                account.provider(),
                                account.providerAccountId(),
                                account.emailAtLink(),
                                account.createdAt())
                        .provider()
                        .equals(OAuthProvider.GOOGLE))
                .isTrue();
    }
}
