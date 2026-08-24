package com.vn.otech.profile;

public record ProfileResponse(String userId, String email, String fullName, String avatarUrl, String bio,
                              String contactInfo, String role) {
}
