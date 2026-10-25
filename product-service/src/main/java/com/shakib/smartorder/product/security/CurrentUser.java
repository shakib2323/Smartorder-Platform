package com.shakib.smartorder.product.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import com.shakib.smartorder.product.exception.ApiException;

// The caller, read once from the verified JWT: user id (the token subject) and role.
public record CurrentUser(long id, boolean admin, boolean seller) {

    public static CurrentUser from(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) 
        {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        long id;
        try {
            id = Long.parseLong(jwt.getSubject());
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token subject");
        }
        if (id <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token subject");
        }
        boolean admin = hasRole(authentication, "ROLE_ADMIN");
        boolean seller = hasRole(authentication, "ROLE_SELLER");
        return new CurrentUser(id, admin, seller);
    }

    private static boolean hasRole(Authentication authentication, String role)
    {
        return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }
}
