package com.vn.otech.profile.dto;

public record ProfileResponse(String userId, String email, String fullName, String avatarUrl, String bio,
                              String contactInfo, String role) {
}