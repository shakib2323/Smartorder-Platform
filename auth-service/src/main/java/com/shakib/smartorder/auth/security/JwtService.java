package com.shakib.smartorder.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.shakib.smartorder.auth.config.JwtProperties;
import com.shakib.smartorder.auth.entity.User;
import com.shakib.smartorder.auth.exception.InvalidTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Creates and checks the signed login tokens (JWT, HS256).
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) 
    {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // Fail at startup, not on the first login.
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters long");
        }
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(User user) 
    {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(properties.expirationMinutes()))))
                .signWith(key)
                .compact();
    }

    // Checks signature, issuer and expiry. Anything wrong is reported as one InvalidTokenException. 
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
    }

    public long expiresInSeconds() 
    {
        return Duration.ofMinutes(properties.expirationMinutes()).toSeconds();
    }
}
