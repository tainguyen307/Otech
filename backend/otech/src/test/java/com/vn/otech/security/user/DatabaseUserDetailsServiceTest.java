package com.vn.otech.security.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Permission;
import com.vn.otech.entity.Role;
import com.vn.otech.entity.RoleName;
import com.vn.otech.entity.RolePermission;
import com.vn.otech.entity.User;
import com.vn.otech.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsernameMapsRolePermissionsAndAccountStatus() {
        User user = user(AccountStatus.ACTIVE);
        Role role = user.getRole();
        Permission permission = new Permission();
        permission.setCode("POST_CREATE");
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        role.getRolePermissions().add(rolePermission);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserDetails details = new DatabaseUserDetailsService(userRepository).loadUserByUsername("user@example.com");

        assertEquals("user@example.com", details.getUsername());
        assertEquals("encoded-password", details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("POST_CREATE")));
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
    }

    @Test
    void deletedUserIsDisabledAndLockedUserIsAccountLocked() {
        User deleted = user(AccountStatus.DELETED);
        User locked = user(AccountStatus.LOCKED);

        UserDetails deletedDetails = DatabaseUserDetailsService.toUserDetails(deleted);
        UserDetails lockedDetails = DatabaseUserDetailsService.toUserDetails(locked);

        assertFalse(deletedDetails.isEnabled());
        assertTrue(deletedDetails.isAccountNonLocked());
        assertTrue(lockedDetails.isEnabled());
        assertFalse(lockedDetails.isAccountNonLocked());
    }

    @Test
    void loadUserByUsernameRejectsUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> new DatabaseUserDetailsService(userRepository).loadUserByUsername("missing@example.com"));
    }

    private User user(AccountStatus status) {
        Role role = new Role();
        role.setName(RoleName.USER);
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}