package com.vn.otech.management.controller;

import com.vn.otech.entity.RoleName;
import com.vn.otech.management.dto.AccountStatusRequest;
import com.vn.otech.management.dto.ChangeUserRoleRequest;
import com.vn.otech.management.dto.CreateManagerRequest;
import com.vn.otech.management.dto.ManagedUserResponse;
import com.vn.otech.management.service.ManagementService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ManagementService managementService;

    public AdminController(ManagementService managementService) {
        this.managementService = managementService;
    }

    @PostMapping("/managers")
    public ResponseEntity<ManagedUserResponse> createManager(
            @Valid @RequestBody CreateManagerRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ManagedUserResponse manager = managementService.createManager(request, principal.getUsername());
        return ResponseEntity.created(URI.create("/api/manager/users?role=MANAGER")).body(manager);
    }

    @PatchMapping("/managers/{managerId}/status")
    public ManagedUserResponse updateManagerStatus(
            @PathVariable UUID managerId,
            @Valid @RequestBody AccountStatusRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return managementService.updateManagerStatus(managerId, request.status(), principal.getUsername());
    }

    @DeleteMapping("/managers/{managerId}")
    public ResponseEntity<Void> deleteManager(
            @PathVariable UUID managerId, @AuthenticationPrincipal UserDetails principal) {
        managementService.deleteManager(managerId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/role")
    public ManagedUserResponse changeUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeUserRoleRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        RoleName role = request.role();
        return managementService.changeUserRole(userId, role, principal.getUsername());
    }
}
