package com.vn.otech.auth;

import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.PasswordResetToken;
import com.vn.otech.entity.RoleName;
import com.vn.otech.entity.User;
import com.vn.otech.repository.PasswordResetTokenRepository;
import com.vn.otech.repository.RoleRepository;
import com.vn.otech.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
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
    private final long resetTokenExpirationMinutes;
    private final boolean exposeResetToken;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordResetTokenRepository resetTokenRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.auth.reset-token-expiration-minutes}") long resetTokenExpirationMinutes,
                       @Value("${app.auth.expose-reset-token}") boolean exposeResetToken) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.resetTokenExpirationMinutes = resetTokenExpirationMinutes;
        this.exposeResetToken = exposeResetToken;
    }

    public AuthResponse register(RegisterRequest request) {
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
        user = userRepository.save(user);
        return createResponse(user);
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (user.getStatus() != AccountStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return createResponse(user);
    }

    public String requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null) {
            return null;
        }
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hash(rawToken));
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(resetTokenExpirationMinutes));
        resetTokenRepository.save(resetToken);
        return exposeResetToken ? rawToken : null;
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
        return new AuthResponse(token, "Bearer", user.getId().toString(), user.getEmail(),
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