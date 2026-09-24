package com.vn.otech.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.PasswordResetToken;
import com.vn.otech.entity.Role;
import com.vn.otech.entity.RoleName;
import com.vn.otech.entity.SignupOtpToken;
import com.vn.otech.entity.User;
import com.vn.otech.repository.PasswordResetTokenRepository;
import com.vn.otech.repository.RoleRepository;
import com.vn.otech.repository.SignupOtpTokenRepository;
import com.vn.otech.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordResetTokenRepository resetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private SignupOtpTokenRepository signupOtpTokenRepository;
    @Mock private MailService mailService;

    @Test
    void registerNormalizesUserAndIssuesSignupOtp() {
        Role role = role();
        when(userRepository.existsByEmailIgnoreCase(" New@Example.com ")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service(5).register(new RegisterRequest("  New User  ", " New@Example.com ", "password123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("New User", savedUser.getFullName());
        assertEquals("new@example.com", savedUser.getEmail());
        assertEquals("hash:password123", savedUser.getPasswordHash());
        assertEquals(AccountStatus.ACTIVE, savedUser.getStatus());
        assertFalse(savedUser.isEmailVerified());

        ArgumentCaptor<SignupOtpToken> tokenCaptor = ArgumentCaptor.forClass(SignupOtpToken.class);
        verify(signupOtpTokenRepository).save(tokenCaptor.capture());
        String otpHash = tokenCaptor.getValue().getCodeHash();
        assertTrue(otpHash.startsWith("hash:"));
        verify(mailService).sendSignupOtp("new@example.com", otpHash.substring("hash:".length()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service(5).register(new RegisterRequest("User", "existing@example.com", "password123")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateReturnsJwtForVerifiedActiveUser() {
        User user = user();
        user.setEmailVerified(true);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash:password123")).thenReturn(true);
        when(jwtService.generateToken(any(), any(UUID.class))).thenReturn("jwt-token");

        AuthResponse response = service(5).authenticate(new AuthRequest(" User@Example.com ", "password123"));

        assertEquals("jwt-token", response.token());
        assertEquals(user.getId().toString(), response.userId());
        assertEquals("user@example.com", response.email());
        assertEquals("USER", response.role());
        assertTrue(response.authorities().contains("ROLE_USER"));
    }

    @Test
    void authenticateLocksAfterConfiguredFailedAttempts() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        AuthService authService = service(2);

        assertThrows(BadCredentialsException.class,
                () -> authService.authenticate(new AuthRequest("user@example.com", "wrong")));
        assertThrows(BadCredentialsException.class,
                () -> authService.authenticate(new AuthRequest("user@example.com", "wrong")));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate(new AuthRequest("user@example.com", "wrong")));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }

    @Test
    void verifySignupMarksTokenAndUserVerified() {
        User user = user();
        SignupOtpToken token = new SignupOtpToken();
        token.setUser(user);
        token.setCodeHash("hash:123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(signupOtpTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", "hash:123456")).thenReturn(true);
        when(jwtService.generateToken(any(), any(UUID.class))).thenReturn("jwt-token");

        AuthResponse response = service(5).verifySignup(new VerifySignupRequest("user@example.com", "123456"));

        assertTrue(user.isEmailVerified());
        assertNotNull(token.getUsedAt());
        verify(signupOtpTokenRepository).save(token);
        verify(userRepository).save(user);
        assertEquals("jwt-token", response.token());
    }

    @Test
    void verifySignupRejectsExpiredToken() {
        User user = user();
        SignupOtpToken token = new SignupOtpToken();
        token.setUser(user);
        token.setCodeHash("hash:123456");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(signupOtpTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(token));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service(5).verifySignup(new VerifySignupRequest("user@example.com", "123456")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(user);
    }

    @Test
    void resendSignupOtpSendsNewCodeForUnverifiedUser() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hash:" + invocation.getArgument(0));

        service(5).resendSignupOtp(" User@Example.com ");

        verify(signupOtpTokenRepository).save(any(SignupOtpToken.class));
        verify(mailService).sendSignupOtp(eq("user@example.com"), anyString());
    }

    @Test
    void resendSignupOtpRejectsAlreadyVerifiedUser() {
        User user = user();
        user.setEmailVerified(true);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service(5).resendSignupOtp("user@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(mailService, never()).sendSignupOtp(anyString(), anyString());
    }

    @Test
    void resetPasswordUpdatesUserAndConsumesToken() {
        User user = user();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash("ef92b778bafe771e89245b89ecbc08a44a4e166c066599e7e0e6c8b3a6f5d3c4");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(resetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service(5).resetPassword(new ResetPasswordRequest("raw-token", "new-password"));

        assertEquals("new-hash", user.getPasswordHash());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
        verify(resetTokenRepository).save(token);
    }

    @Test
    void resetPasswordRejectsUnknownToken() {
        when(resetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service(5).resetPassword(new ResetPasswordRequest("bad-token", "new-password")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void requestPasswordResetDoesNothingForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        service(5).requestPasswordReset(new ForgotPasswordRequest("missing@example.com"));

        verify(resetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(mailService, never()).sendPasswordResetOtp(anyString(), anyString());
    }

    @Test
    void requestPasswordResetSavesTokenAndSendsCodeForKnownUser() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        service(5).requestPasswordReset(new ForgotPasswordRequest("user@example.com"));

        verify(resetTokenRepository).save(any(PasswordResetToken.class));
        verify(mailService).sendPasswordResetOtp(eq("user@example.com"), anyString());
    }

    private AuthService service(int maxFailures) {
        return new AuthService(userRepository, roleRepository, resetTokenRepository, passwordEncoder, jwtService,
                30, signupOtpTokenRepository, mailService, 10, maxFailures, 15);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setPasswordHash("hash:password123");
        user.setRole(role());
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    private Role role() {
        Role role = new Role();
        role.setName(RoleName.USER);
        return role;
    }
}