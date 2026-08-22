package com.vn.otech.auth;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final RevokedTokenStore revokedTokenStore;

    public AuthController(AuthService authService, JwtService jwtService, RevokedTokenStore revokedTokenStore) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.revokedTokenStore = revokedTokenStore;
    }

    @PostMapping("/signup")
    public Map<String, String> signup(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Map.of("message", "Verification code sent to your email");
    }

    @PostMapping("/verify-signup")
    public AuthResponse verifySignup(@Valid @RequestBody VerifySignupRequest request) {
        return authService.verifySignup(request);
    }

    @PostMapping("/resend-signup-otp")
    public Map<String, String> resendSignupOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendSignupOtp(request.email());
        return Map.of("message", "Verification code sent to your email");
    }

    @PostMapping({"/signin", "/login"})
    public AuthResponse signin(@Valid @RequestBody AuthRequest request) {
        return authService.authenticate(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                revokedTokenStore.revoke(token, jwtService.parse(token).getExpiration().toInstant());
            } catch (RuntimeException ignored) {
                // Logout remains idempotent for missing or expired tokens.
            }
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String resetToken = authService.requestPasswordReset(request);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "If the email exists, password reset instructions have been created");
        if (resetToken != null) {
            response.put("resetToken", resetToken);
        }
        return response;
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Map.of("message", "Password reset successfully");
    }
}