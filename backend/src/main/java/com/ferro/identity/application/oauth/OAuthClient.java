package com.ferro.identity.application.oauth;

import com.ferro.identity.domain.OAuthProvider;
import com.ferro.identity.domain.OAuthUser;

/** Puerto de salida hacia Google/GitHub. Los adapters hacen el HTTP real. */
public interface OAuthClient {

    OAuthProvider provider();

    OAuthUser fetchUser(String code, String codeVerifier);
}
