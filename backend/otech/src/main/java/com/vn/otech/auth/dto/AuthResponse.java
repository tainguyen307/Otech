package com.vn.otech.auth.dto;

import java.util.List;

public record AuthResponse(String token, String tokenType, String userId, String email, String fullName, String avatarUrl, String role,
                           List<String> authorities) {
}