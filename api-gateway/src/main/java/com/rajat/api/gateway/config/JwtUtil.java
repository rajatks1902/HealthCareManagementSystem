package com.rajat.api.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component // Marks this class as a Spring-managed component for JWT operations
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}") // Injects the JWT secret key from application configuration
    private String jwtSecret;

    private Key key; // Holds the secure key for token signing and validation

    /**
     * Initializes the secure key using the secret string.
     * The `@PostConstruct` annotation ensures this method runs after dependency injection.
     */
    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret key is not configured or is empty.");
        }
        // Decode the Base64-encoded secret and create the key
//        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }


    /**
     * Extracts all claims from a given JWT token.
     *
     * @param token the JWT token.
     * @return the claims extracted from the token.
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(parseJwt(token))
                .getBody();
    }

    private String parseJwt(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new JwtException("Authorization header must use the Bearer scheme");
        }
        return token.substring(7);

    }

    /**
     * Checks if the JWT token is expired.
     *
     * @param token the JWT token.
     * @return true if the token is expired, false otherwise.
     */
    private boolean isTokenExpired(String token) {
        return getAllClaimsFromToken(token).getExpiration().before(new Date());
    }

    /**
     * Validates if the token is invalid (e.g., expired).
     *
     * @param token the JWT token.
     * @return true if the token is invalid, false otherwise.
     */
    public boolean isInvalid(String token) {
        try {
            return isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Rejecting invalid JWT: {}", ex.getMessage());
            return true;
        }
    }
}
