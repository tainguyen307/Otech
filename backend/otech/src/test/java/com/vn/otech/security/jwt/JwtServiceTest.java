package com.vn.otech.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {
    private static final String SECRET = "a-very-long-test-secret-that-is-at-least-32-bytes";

    @Test
    void generateTokenRoundTripsClaims() {
        JwtService service = new JwtService(properties(60_000));
        UUID userId = UUID.randomUUID();
        UserDetails user = User.withUsername("user@example.com")
                .password("ignored")
                .authorities("ROLE_USER", "POST_READ")
                .build();

        Claims claims = service.parse(service.generateToken(user, userId));

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals("user@example.com", claims.get("email"));
        assertTrue(((List<?>) claims.get("authorities")).containsAll(List.of("ROLE_USER", "POST_READ")));
    }

    @Test
    void parseRejectsTokenSignedByAnotherKey() {
        JwtService issuer = new JwtService(properties(60_000));
        JwtProperties otherProperties = properties(60_000);
        otherProperties.setSecret("another-long-test-secret-that-is-at-least-32-bytes");
        JwtService verifier = new JwtService(otherProperties);
        UserDetails user = User.withUsername("user@example.com").password("ignored").authorities("ROLE_USER").build();

        String token = issuer.generateToken(user, UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> verifier.parse(token));
    }

    @Test
    void parseRejectsExpiredToken() {
        JwtService service = new JwtService(properties(-1));
        UserDetails user = User.withUsername("user@example.com").password("ignored").authorities("ROLE_USER").build();
        String token = service.generateToken(user, UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> service.parse(token));
    }

    private JwtProperties properties(long expirationMs) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMs(expirationMs);
        return properties;
    }
}