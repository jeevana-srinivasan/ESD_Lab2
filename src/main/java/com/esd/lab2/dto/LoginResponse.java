package com.esd.lab2.dto;

/**
 * Response returned after successful authentication.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    /**
     * Convenience constructor so tokenType is always "Bearer".
     */
    public LoginResponse(String accessToken, long expiresIn) {
        this(accessToken, "Bearer", expiresIn);
    }
}