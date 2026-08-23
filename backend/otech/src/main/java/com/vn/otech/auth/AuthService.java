package com.vn.otech.auth;

import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.PasswordResetToken;
import com.vn.otech.entity.RoleName;
import com.vn.otech.entity.SignupOtpToken;
import com.vn.otech.entity.User;
import com.vn.otech.repository.PasswordResetTokenRepository;
import com.vn.otech.repository.RoleRepository;
import com.vn.otech.repository.SignupOtpTokenRepository;
import com.vn.otech.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SignupOtpTokenRepository signupOtpTokenRepository;
    private final MailService mailService;
    private final long resetTokenExpirationMinutes;
    private final long signupOtpExpirationMinutes;
    private final int loginMaxFailures;
    private final long loginLockoutMinutes;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, LoginFailures> loginFailures = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordResetTokenRepository resetTokenRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.auth.reset-token-expiration-minutes}") long resetTokenExpirationMinutes,
                       SignupOtpTokenRepository signupOtpTokenRepository, MailService mailService,
                       @Value("${app.auth.signup-otp-expiration-minutes}") long signupOtpExpirationMinutes,
                       @Value("${app.auth.login-max-failures}") int loginMaxFailures,
                       @Value("${app.auth.login-lockout-minutes}") long loginLockoutMinutes) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.signupOtpTokenRepository = signupOtpTokenRepository;
        this.mailService = mailService;
        this.resetTokenExpirationMinutes = resetTokenExpirationMinutes;
        this.signupOtpExpirationMinutes = signupOtpExpirationMinutes;
        this.loginMaxFailures = loginMaxFailures;
        this.loginLockoutMinutes = loginLockoutMinutes;
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "USER role is missing")));
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(false);
        user = userRepository.save(user);
        issueSignupOtp(user);
    }

    public AuthResponse authenticate(AuthRequest request) {
        String email = request.email().trim().toLowerCase();
        LoginFailures failures = loginFailures.get(email);
        if (failures != null && failures.lockedUntil() != null
            && failures.lockedUntil().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again later.");
        }
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.getStatus() != AccountStatus.ACTIVE || !user.isEmailVerified()
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordLoginFailure(email);
            throw new BadCredentialsException("Invalid email or password");
        }
        loginFailures.remove(email);
        return createResponse(user);
    }

    public AuthResponse verifySignup(VerifySignupRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code"));
        SignupOtpToken token = signupOtpTokenRepository.findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId())
                .filter(candidate -> candidate.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code"));
        if (!passwordEncoder.matches(request.otp(), token.getCodeHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
        }
        token.setUsedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        signupOtpTokenRepository.save(token);
        userRepository.save(user);
        return createResponse(user);
    }

    public void resendSignupOtp(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signup verification is unavailable"));
        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already verified");
        }
        issueSignupOtp(user);
    }

    private void issueSignupOtp(User user) {
        int code = 100000 + secureRandom.nextInt(900000);
        SignupOtpToken token = new SignupOtpToken();
        token.setUser(user);
        token.setCodeHash(passwordEncoder.encode(Integer.toString(code)));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(signupOtpExpirationMinutes));
        signupOtpTokenRepository.save(token);
        mailService.sendSignupOtp(user.getEmail(), Integer.toString(code));
    }

    private void recordLoginFailure(String email) {
        loginFailures.compute(email, (key, current) -> {
            LocalDateTime now = LocalDateTime.now();
            if (current == null || (current.lockedUntil() != null && current.lockedUntil().isBefore(now))) {
                return new LoginFailures(1, null);
            }
            int attempts = current.attempts() + 1;
            return new LoginFailures(attempts, attempts >= loginMaxFailures
                    ? now.plusMinutes(loginLockoutMinutes) : current.lockedUntil());
        });
    }

    private record LoginFailures(int attempts, LocalDateTime lockedUntil) { }

    public void requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null) {
            return;
        }
        String rawToken = Integer.toString(100000 + secureRandom.nextInt(900000));
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hash(rawToken));
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(resetTokenExpirationMinutes));
        resetTokenRepository.save(resetToken);
        mailService.sendPasswordResetOtp(user.getEmail(), rawToken);
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(hash(request.token()))
                .filter(token -> token.getUsedAt() == null && token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsedAt(LocalDateTime.now());
        userRepository.save(user);
        resetTokenRepository.save(resetToken);
    }

    private AuthResponse createResponse(User user) {
        UserDetails details = DatabaseUserDetailsService.toUserDetails(user);
        String token = jwtService.generateToken(details, user.getId());
        List<String> authorities = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        return new AuthResponse(token, "Bearer", user.getId().toString(), user.getEmail(), user.getFullName(), user.getAvatarUrl(),
                user.getRole().getName().name(), authorities);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}