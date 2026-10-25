package com.shakib.smartorder.product.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

@Configuration
public class ProductSecurityConfig 
{

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception 
    {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/products/**").permitAll()

                        // Seller area. These come BEFORE the public GET rules so "mine"/"me" are not read as ids.
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/mine").hasRole("SELLER")
                        .requestMatchers("/api/v1/brands/me", "/api/v1/brands/me/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/brands").hasRole("SELLER")

                        // Admin area: brand approval.
                        .requestMatchers("/api/v1/brands/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/brands/*/approve", "/api/v1/brands/*/suspend")
                                .hasRole("ADMIN")

                        // Public catalogue.
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/*",
                                "/api/v1/brands", "/api/v1/brands/*").permitAll()

                        // Product writes: a seller (own brand only, enforced in the service) or an admin.
                        .requestMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/products/**")
                                .hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAnyRole("SELLER", "ADMIN")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.issuer:smartorder-auth}") String issuer)
    {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() 
    {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || !(role.equals("ADMIN") || role.equals("SELLER") || role.equals("CUSTOMER"))) 
            {
                return java.util.List.of();
            }
            return java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}
