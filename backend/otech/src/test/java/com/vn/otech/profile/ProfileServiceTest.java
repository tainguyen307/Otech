package com.vn.otech.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.vn.otech.entity.Role;
import com.vn.otech.entity.RoleName;
import com.vn.otech.entity.User;
import com.vn.otech.repository.UserRepository;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ObjectProvider<Cloudinary> cloudinaryProvider;
    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;

    @Test
    void updateTrimsProfileFieldsAndChangesPassword() {
        User user = user();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        ProfileResponse response = service().update("user@example.com",
                new ProfileUpdateRequest("  New Name  ", "  About me  ", "  0123  ",
                        "old-password", "new-password", "new-password"));

        assertEquals("New Name", response.fullName());
        assertEquals("About me", response.bio());
        assertEquals("0123", response.contactInfo());
        assertEquals("new-hash", user.getPasswordHash());
    }

    @Test
    void updateRejectsPartialPasswordChange() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().update("user@example.com",
                        new ProfileUpdateRequest("Name", null, null, "old-password", null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateRejectsUnknownUser() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().update("missing@example.com",
                        new ProfileUpdateRequest("Name", null, null, null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void uploadAvatarStoresCloudinarySecureUrl() throws IOException {
        User user = user();
        when(cloudinaryProvider.getIfAvailable()).thenReturn(cloudinary);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", "https://img.test/avatar.png"));
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        MultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {1, 2, 3});

        ProfileResponse response = service().uploadAvatar("user@example.com", file);

        assertEquals("https://img.test/avatar.png", response.avatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    void uploadAvatarRejectsNonImageFiles() {
        MultipartFile file = new MockMultipartFile("avatar", "notes.txt", "text/plain", new byte[] {1});

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().uploadAvatar("user@example.com", file));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void uploadAvatarReturnsServiceUnavailableWhenCloudinaryIsMissing() {
        when(cloudinaryProvider.getIfAvailable()).thenReturn(null);
        MultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {1});

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().uploadAvatar("user@example.com", file));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void uploadAvatarReturnsBadGatewayWhenCloudinaryUploadFails() throws IOException {
        when(cloudinaryProvider.getIfAvailable()).thenReturn(cloudinary);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("upload failed"));
        MultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {1});

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().uploadAvatar("user@example.com", file));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    private ProfileService service() {
        return new ProfileService(userRepository, passwordEncoder, cloudinaryProvider);
    }

    private User user() {
        Role role = new Role();
        role.setName(RoleName.USER);
        User user = new User();
        user.setId(java.util.UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFullName("Old Name");
        user.setPasswordHash("old-hash");
        user.setRole(role);
        return user;
    }
}