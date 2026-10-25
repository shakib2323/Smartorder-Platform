package com.shakib.smartorder.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shakib.smartorder.auth.dto.AuthResponse;
import com.shakib.smartorder.auth.dto.LoginRequest;
import com.shakib.smartorder.auth.dto.RegisterRequest;
import com.shakib.smartorder.auth.dto.UserResponse;
import com.shakib.smartorder.auth.entity.User;
import com.shakib.smartorder.auth.enums.Role;
import com.shakib.smartorder.auth.exception.EmailAlreadyUsedException;
import com.shakib.smartorder.auth.exception.InvalidCredentialsException;
import com.shakib.smartorder.auth.exception.InvalidRequestException;
import com.shakib.smartorder.auth.exception.InvalidTokenException;
import com.shakib.smartorder.auth.exception.TooManyAttemptsException;
import com.shakib.smartorder.auth.repository.UserRepository;
import com.shakib.smartorder.auth.security.JwtService;
import com.shakib.smartorder.auth.security.LoginAttemptService;

import io.jsonwebtoken.Claims;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // BCrypt only looks at the first 72 bytes, so longer passwords are not accepted.
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final String BEARER = "Bearer ";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;

    // A real hash that matches nothing. Login compares against it when the email is unknown,
    // so unknown and known emails take the same time and the response cannot be used to probe them.
    private final String unknownUserHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            LoginAttemptService loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.unknownUserHash = passwordEncoder.encode("this-password-belongs-to-nobody");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) 
    {
        return registerWithRole(request, Role.CUSTOMER);
    }

    // Creates a SELLER account. The seller then registers a brand in the product service. 
    @Transactional
    public UserResponse registerSeller(RegisterRequest request) 
    {
        return registerWithRole(request, Role.SELLER);
    }

    private UserResponse registerWithRole(RegisterRequest request, Role role) 
    {
        if (tooLong(request.password())) 
        {
            throw new InvalidRequestException("password must be at most " + MAX_PASSWORD_BYTES + " bytes long");
        }
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email))
        {
            throw new EmailAlreadyUsedException();
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), role);
        try {
            User saved = userRepository.save(user);
            log.info("Registered {} {}", role, saved.getId());
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // Two sign-ups with the same email at the same moment: the unique key let one win.
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request)
    {
        String email = normalize(request.email());
        if (loginAttempts.isLocked(email)) 
        {
            throw new TooManyAttemptsException();
        }
        User user = userRepository.findByEmail(email).orElse(null);

        String hashToCheck = user != null ? user.getPasswordHash() : unknownUserHash;
        boolean passwordMatches = !tooLong(request.password())
                && passwordEncoder.matches(request.password(), hashToCheck);

        if (user == null || !passwordMatches)
        {
            loginAttempts.recordFailure(email);
            log.warn("Failed login attempt");
            throw new InvalidCredentialsException();
        }
        loginAttempts.recordSuccess(email);

        log.info("User {} logged in", user.getId());
        return new AuthResponse(jwtService.createToken(user), "Bearer", jwtService.expiresInSeconds(),
                UserResponse.from(user));
    }

    // Who does this token belong to? Takes the raw Authorization header value.
    @Transactional(readOnly = true)
    public UserResponse currentUser(String authorizationHeader) 
    {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER)) 
        {
            throw new InvalidTokenException();
        }
        Claims claims = jwtService.parse(authorizationHeader.substring(BEARER.length()).trim());

        long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidTokenException();
        }
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(InvalidTokenException::new);
    }

    private String normalize(String email)
    {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean tooLong(String password)
    {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
    }
}
