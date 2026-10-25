package com.shakib.smartorder.order.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import com.shakib.smartorder.order.exception.UnauthorizedException;

// Reads who is calling from the verified JWT. One place instead of a copy in every controller.
public final class Caller {

    private Caller() {
    }

    // The user id (token subject). A missing or malformed subject is a 401, never a 500. 
    public static long id(Jwt jwt) 
    {
        try {
            long id = Long.parseLong(jwt.getSubject());
            if (id > 0) {
                return id;
            }
        } catch (RuntimeException ignored)
        {
            // falls through to the 401 below
        }
        throw new UnauthorizedException("Invalid token subject");
    }

    public static boolean isAdmin(Authentication authentication)
    {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
