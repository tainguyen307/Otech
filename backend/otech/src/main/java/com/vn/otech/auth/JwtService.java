package com.vn.otech.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = properties.getExpirationMs();
    }

    public String generateToken(UserDetails userDetails, UUID userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", userDetails.getUsername())
                .claim("authorities", userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}