package com.esd.lab2.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenDenyList {

    private final ConcurrentHashMap<String, Instant> revokedTokens =
            new ConcurrentHashMap<>();

    /**
     * Revokes a token until its natural JWT expiration time.
     */
    public void revoke(String jti, Instant expiry) {
        cleanExpired();
        revokedTokens.put(jti, expiry);
    }

    /**
     * Returns true if the token identified by this jti
     * has been revoked and has not yet naturally expired.
     */
    public boolean isRevoked(String jti) {
        cleanExpired();
        return revokedTokens.containsKey(jti);
    }

    /**
     * Removes denylist entries whose JWTs have already expired.
     */
    public void cleanExpired() {

        Instant now = Instant.now();

        revokedTokens.entrySet().removeIf(entry ->
                entry.getValue().isBefore(now)
                        || entry.getValue().equals(now)
        );
    }
}
