package com.shakib.smartorder.auth.dto;

// expiresIn is in seconds. Send the token back as:  Authorization: Bearer <accessToken> 
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user) {
}
