package com.ferro.platform.ratelimit;

import java.time.Duration;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.ferro.platform.api.RateLimiter;
import com.ferro.shared.domain.RateLimitExceededException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

/**
 * Límites de RN-20 en memoria. En MVP hay una sola instancia; si se escala horizontalmente
 * habrá que mover los buckets a un almacén compartido (Redis/JCache).
 */
@Component
public class Bucket4jRateLimiter implements RateLimiter {

    private static final long RETRY_AFTER_SECONDS = 60;
    private static final long REGISTER_PER_HOUR = 5;
    private static final long LOGIN_PER_MINUTE = 10;
    private static final long REFRESH_PER_MINUTE = 30;
    private static final long PASSWORD_RESET_PER_HOUR = 3;
    private static final long AUTHENTICATED_PER_MINUTE = 100;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(100_000)
            .build();
    private final RateLimitProperties properties;

    public Bucket4jRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    public void checkRegister(String clientIp) {
        consume("register:" + clientIp, REGISTER_PER_HOUR, Duration.ofHours(1));
    }

    @Override
    public void checkLogin(String clientIp, String email) {
        consume("login:" + clientIp + ":" + normalize(email), LOGIN_PER_MINUTE, Duration.ofMinutes(1));
    }

    @Override
    public void checkRefresh(String clientIp) {
        consume("refresh:" + clientIp, REFRESH_PER_MINUTE, Duration.ofMinutes(1));
    }

    @Override
    public void checkPasswordReset(String email) {
        consume("password-reset:" + normalize(email), PASSWORD_RESET_PER_HOUR, Duration.ofHours(1));
    }

    @Override
    public void checkAuthenticated(String userId) {
        consume("api:" + userId, AUTHENTICATED_PER_MINUTE, Duration.ofMinutes(1));
    }

    private void consume(String key, long tokens, Duration period) {
        if (!properties.enabled()) {
            return;
        }
        Bucket bucket = buckets.get(
                key,
                ignored -> Bucket.builder()
                        .addLimit(Bandwidth.classic(tokens, io.github.bucket4j.Refill.greedy(tokens, period)))
                        .build());
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(RETRY_AFTER_SECONDS);
        }
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }
}
