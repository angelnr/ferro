package com.ferro.shared.domain;

/** Se ha superado un límite de peticiones (429 RATE_LIMITED, RN-20). */
public final class RateLimitExceededException extends DomainException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Demasiadas peticiones; inténtalo más tarde");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
