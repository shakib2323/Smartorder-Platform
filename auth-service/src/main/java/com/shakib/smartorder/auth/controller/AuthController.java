package com.shakib.smartorder.auth.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shakib.smartorder.auth.dto.AuthResponse;
import com.shakib.smartorder.auth.dto.LoginRequest;
import com.shakib.smartorder.auth.dto.RegisterRequest;
import com.shakib.smartorder.auth.dto.UserResponse;
import com.shakib.smartorder.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    // Creates a customer account. Log in afterwards to get a token. 
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) 
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    // Creates a seller account (a brand owner). Log in, then register the brand.
    @PostMapping("/register-seller")
    public ResponseEntity<UserResponse> registerSeller(@Valid @RequestBody RegisterRequest request)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registerSeller(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) 
    {
        // A response that carries a token must never be cached by a browser or proxy.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.login(request));
    }

    // Handy for checking a token: returns the account the Bearer token belongs to.
    @GetMapping("/me")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String authorization)
    {
        return service.currentUser(authorization);
    }
}
