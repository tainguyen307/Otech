package com.vn.otech.auth;

import java.util.List;

public record AuthResponse(String token, String tokenType, String userId, String email, String role,
                           List<String> authorities) {
}