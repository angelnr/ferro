package com.ferro.identity.application.oauth;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ferro.identity.domain.OAuthLoginException;
import com.ferro.identity.domain.OAuthProvider;

@Component
public class OAuthClientRegistry {

    private final Map<OAuthProvider, OAuthClient> clients = new EnumMap<>(OAuthProvider.class);

    public OAuthClientRegistry(List<OAuthClient> clients) {
        for (OAuthClient client : clients) {
            this.clients.put(client.provider(), client);
        }
    }

    public OAuthClient clientFor(OAuthProvider provider) {
        OAuthClient client = clients.get(provider);
        if (client == null) {
            throw new OAuthLoginException("Proveedor OAuth no disponible: " + provider);
        }
        return client;
    }
}
