package com.vn.otech.auth;

import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.RolePermission;
import com.vn.otech.entity.User;
import com.vn.otech.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return toUserDetails(user);
    }

    public static UserDetails toUserDetails(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name()));
        for (RolePermission rolePermission : user.getRole().getRolePermissions()) {
            authorities.add(new SimpleGrantedAuthority(rolePermission.getPermission().getCode()));
        }
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(user.getStatus() == AccountStatus.DELETED)
                .accountLocked(user.getStatus() == AccountStatus.LOCKED)
                .build();
    }
}