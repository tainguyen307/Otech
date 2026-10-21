package com.vn.otech.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {
    private static final String KEY_PREFIX = "auth:login:";
    private static final DefaultRedisScript<Long> RECORD_FAILURE_SCRIPT = new DefaultRedisScript<>(
            """
            local attempts = redis.call('INCR', KEYS[1])
            if attempts == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return attempts
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final int maxFailures;
    private final Duration rateLimitWindow;

    public LoginAttemptService(StringRedisTemplate redisTemplate,
                               @Value("${app.auth.login-max-failures}") int maxFailures,
                               @Value("${app.auth.login-lockout-minutes}") long rateLimitWindowMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxFailures = maxFailures;
        this.rateLimitWindow = Duration.ofMinutes(rateLimitWindowMinutes);
    }

    public boolean isRateLimited(String email) {
        String attempts = redisTemplate.opsForValue().get(attemptsKey(email));
        return attempts != null && Long.parseLong(attempts) >= maxFailures;
    }

    public void recordFailure(String email) {
        redisTemplate.execute(
                RECORD_FAILURE_SCRIPT,
                java.util.Collections.singletonList(attemptsKey(email)),
                Long.toString(rateLimitWindow.toSeconds()));
    }

    public void clearFailures(String email) {
        redisTemplate.delete(attemptsKey(email));
    }

    private String attemptsKey(String email) {
        return KEY_PREFIX + "attempts:" + hash(email);
    }

    private String hash(String email) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(email.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
