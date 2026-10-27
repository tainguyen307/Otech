package com.vn.otech.security.web;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vn.otech.security.jwt.JwtService;
import com.vn.otech.security.jwt.RevokedTokenStore;
import com.vn.otech.security.user.DatabaseUserDetailsService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.mock.web.MockFilterChain;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock private JwtService jwtService;
    @Mock private DatabaseUserDetailsService userDetailsService;
    @Mock private RevokedTokenStore revokedTokenStore;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lockedAccountTokenDoesNotAuthenticate() throws Exception {
        doFilterForAccount(true);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void activeAccountTokenAuthenticates() throws Exception {
        doFilterForAccount(false);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private void doFilterForAccount(boolean locked) throws Exception {
        String token = "valid-token";
        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn("manager@example.com");
        when(jwtService.parse(token)).thenReturn(claims);
        when(revokedTokenStore.isRevoked(token)).thenReturn(false);
        UserDetails user = User.withUsername("manager@example.com")
                .password("encoded")
                .authorities("ROLE_MANAGER")
                .accountLocked(locked)
                .build();
        when(userDetailsService.loadUserByUsername("manager@example.com")).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        new JwtAuthenticationFilter(jwtService, userDetailsService, revokedTokenStore)
                .doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }
}
