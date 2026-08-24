package com.vn.otech.profile;

import com.cloudinary.Cloudinary;
import com.vn.otech.entity.User;
import com.vn.otech.repository.UserRepository;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<Cloudinary> cloudinaryProvider;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          ObjectProvider<Cloudinary> cloudinaryProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryProvider = cloudinaryProvider;
    }

    public ProfileResponse update(String email, ProfileUpdateRequest request) {
        User user = findUser(email);
        boolean changingPassword = hasText(request.oldPassword()) || hasText(request.newPassword())
                || hasText(request.confirmPassword());
        if (changingPassword) {
            if (!hasText(request.oldPassword()) || !hasText(request.newPassword())
                    || !hasText(request.confirmPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Old password, new password, and password confirmation are required");
            }
            if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password is incorrect");
            }
            if (!request.newPassword().equals(request.confirmPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New passwords do not match");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }
        user.setFullName(request.fullName().trim());
        user.setBio(blankToNull(request.bio()));
        user.setContactInfo(blankToNull(request.contactInfo()));
        return toResponse(userRepository.save(user));
    }

    public ProfileResponse uploadAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getContentType() == null
                || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a valid image");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be 5 MB or smaller");
        }
        Cloudinary cloudinary = cloudinaryProvider.getIfAvailable();
        if (cloudinary == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Image upload is not configured");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), Map.of("folder", "otech/avatars"));
            User user = findUser(email);
            user.setAvatarUrl((String) result.get("secure_url"));
            return toResponse(userRepository.save(user));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload image", exception);
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(user.getId().toString(), user.getEmail(), user.getFullName(), user.getAvatarUrl(),
                user.getBio(), user.getContactInfo(), user.getRole().getName().name());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
