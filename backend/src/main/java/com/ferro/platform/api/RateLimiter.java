package com.ferro.platform.api;

/** Contrato público de rate limiting (RN-20) para los módulos de negocio. */
public interface RateLimiter {

    void checkRegister(String clientIp);

    void checkLogin(String clientIp, String email);

    void checkRefresh(String clientIp);

    void checkPasswordReset(String email);

    void checkAuthenticated(String userId);
}
