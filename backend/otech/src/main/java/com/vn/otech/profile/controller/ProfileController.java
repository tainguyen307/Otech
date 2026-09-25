package com.vn.otech.profile.controller;

import com.vn.otech.profile.dto.ProfileResponse;
import com.vn.otech.profile.dto.ProfileUpdateRequest;
import com.vn.otech.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PatchMapping
    public ProfileResponse update(@AuthenticationPrincipal UserDetails principal,
                                  @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.update(principal.getUsername(), request);
    }

    @PostMapping("/avatar")
    public ProfileResponse uploadAvatar(@AuthenticationPrincipal UserDetails principal,
                                        @RequestPart("file") MultipartFile file) {
        return profileService.uploadAvatar(principal.getUsername(), file);
    }
}