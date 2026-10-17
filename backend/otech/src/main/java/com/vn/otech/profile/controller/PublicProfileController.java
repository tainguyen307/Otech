package com.vn.otech.profile.controller;

import com.vn.otech.profile.dto.PublicProfileResponse;
import com.vn.otech.profile.service.PublicProfileService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class PublicProfileController {
    private final PublicProfileService publicProfileService;

    public PublicProfileController(PublicProfileService publicProfileService) {
        this.publicProfileService = publicProfileService;
    }

    @GetMapping("/{userId}")
    public PublicProfileResponse getProfile(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails principal) {
        return publicProfileService.getProfile(userId, principal == null ? null : principal.getUsername());
    }

    @PostMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID userId) {
        publicProfileService.follow(principal.getUsername(), userId);
    }

    @DeleteMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID userId) {
        publicProfileService.unfollow(principal.getUsername(), userId);
    }
}
