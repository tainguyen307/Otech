package com.vn.otech.management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.RoleRepository;
import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Role;
import com.vn.otech.entity.RoleName;
import com.vn.otech.entity.User;
import com.vn.otech.management.dto.CreateManagerRequest;
import com.vn.otech.management.repository.GroupManagementRepository;
import com.vn.otech.management.repository.ReportManagementRepository;
import com.vn.otech.market.repository.ProductRepository;
import com.vn.otech.post.repository.PostRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ManagementServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PostRepository postRepository;
    @Mock private GroupManagementRepository groupRepository;
    @Mock private ReportManagementRepository reportRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private ManagementService managementService;

    @Test
    void managerCannotCreateAnotherManager() {
        User actor = user(RoleName.MANAGER);
        when(userRepository.findByEmailIgnoreCase("manager@example.com")).thenReturn(Optional.of(actor));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> managementService.createManager(
                        new CreateManagerRequest("New Manager", "new@example.com", "password123"),
                        "manager@example.com"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(roleRepository, never()).findByName(RoleName.MANAGER);
    }

    @Test
    void adminCreatesVerifiedManagerWithEncodedPassword() {
        User actor = user(RoleName.ADMIN);
        Role managerRole = new Role();
        managerRole.setName(RoleName.MANAGER);
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(actor));
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.MANAGER)).thenReturn(Optional.of(managerRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = managementService.createManager(
                new CreateManagerRequest("New Manager", "new@example.com", "password123"),
                "admin@example.com");

        assertEquals(RoleName.MANAGER, result.role());
        assertEquals(AccountStatus.ACTIVE, result.status());
        assertEquals("new@example.com", result.email());
        ArgumentCaptor<User> managerCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(managerCaptor.capture());
        assertTrue(managerCaptor.getValue().isEmailVerified());
        assertEquals("encoded-password", managerCaptor.getValue().getPasswordHash());
    }

    @Test
    void managerCannotLockAnotherManagementAccount() {
        User manager = user(RoleName.MANAGER);
        UUID managerId = UUID.randomUUID();
        when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> managementService.updateUserStatus(managerId, AccountStatus.LOCKED));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deletedUserCannotBeReactivatedThroughStatusEndpoint() {
        User deletedUser = user(RoleName.USER);
        deletedUser.setStatus(AccountStatus.DELETED);
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> managementService.updateUserStatus(userId, AccountStatus.ACTIVE));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    private User user(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Test account");
        user.setEmail(roleName.name().toLowerCase() + "@example.com");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }
}
