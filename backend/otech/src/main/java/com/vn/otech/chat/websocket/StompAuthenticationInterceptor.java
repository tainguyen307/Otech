package com.vn.otech.chat.websocket;

import com.vn.otech.security.jwt.JwtService;
import com.vn.otech.security.jwt.RevokedTokenStore;
import com.vn.otech.security.user.DatabaseUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class StompAuthenticationInterceptor implements ChannelInterceptor {
    private static final List<String> USER_QUEUES = List.of(
            "/user/queue/messages",
            "/user/queue/message-status",
            "/user/queue/errors");

    private final JwtService jwtService;
    private final DatabaseUserDetailsService userDetailsService;
    private final RevokedTokenStore revokedTokenStore;

    public StompAuthenticationInterceptor(JwtService jwtService,
                                          DatabaseUserDetailsService userDetailsService,
                                          RevokedTokenStore revokedTokenStore) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.revokedTokenStore = revokedTokenStore;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            throw new AccessDeniedException("Invalid WebSocket message");
        }

        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
        } else {
            if (accessor.getUser() == null) {
                throw new AccessDeniedException("Authentication is required");
            }
            validateDestination(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Authentication is required");
        }
        String token = authorization.substring("Bearer ".length());
        try {
            if (revokedTokenStore.isRevoked(token)) {
                throw new AccessDeniedException("Authentication is required");
            }
            Claims claims = jwtService.parse(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.get("email", String.class));
            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()
                    || !userDetails.isAccountNonExpired() || !userDetails.isCredentialsNonExpired()) {
                throw new AccessDeniedException("This account cannot start a chat session");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            throw new AccessDeniedException("Authentication is required", exception);
        }
    }

    private void validateDestination(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            if (!USER_QUEUES.contains(destination)) {
                throw new AccessDeniedException("Subscription destination is not allowed");
            }
        } else if (accessor.getCommand() == StompCommand.SEND) {
            if (destination == null || !destination.startsWith("/app/conversations/")) {
                throw new AccessDeniedException("Send destination is not allowed");
            }
        }
    }
}
