package com.esd.lab2.controller;

import com.esd.lab2.dto.LoginRequest;
import com.esd.lab2.dto.LoginResponse;
import com.esd.lab2.dto.SignupRequest;
import com.esd.lab2.dto.UserResponse;
import com.esd.lab2.security.JwtService;
import com.esd.lab2.security.TokenDenyList;
import com.esd.lab2.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenDenyList tokenDenyList;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            TokenDenyList tokenDenyList
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenDenyList = tokenDenyList;
    }

    /**
     * S1 — Signup
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {

        UserResponse response =
                authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * S2 — Login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * S5 — Logout
     *
     * Revokes the particular JWT used for this request.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization")
            String authorizationHeader
    ) {

        String token =
                authorizationHeader.substring(7);

        Claims claims =
                jwtService.parseToken(token);

        String jti =
                claims.getId();

        Instant expiry =
                claims.getExpiration()
                        .toInstant();

        tokenDenyList.revoke(
                jti,
                expiry
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}


