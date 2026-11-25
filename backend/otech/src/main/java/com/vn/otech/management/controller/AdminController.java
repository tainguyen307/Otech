package com.vn.otech.management.controller;

import com.vn.otech.entity.RoleName;
import com.vn.otech.billing.dto.BillingPackageRequest;
import com.vn.otech.billing.dto.BillingPackageResponse;
import com.vn.otech.billing.dto.BillingSettingRequest;
import com.vn.otech.billing.service.BillingService;
import com.vn.otech.management.dto.AccountStatusRequest;
import com.vn.otech.management.dto.ChangeUserRoleRequest;
import com.vn.otech.management.dto.CreateManagerRequest;
import com.vn.otech.management.dto.ManagedUserResponse;
import com.vn.otech.management.service.ManagementService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ManagementService managementService;
    private final BillingService billingService;

    public AdminController(ManagementService managementService, BillingService billingService) {
        this.managementService = managementService;
        this.billingService = billingService;
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

    @GetMapping("/billing/packages")
    public List<BillingPackageResponse> billingPackages() {
        return billingService.listAllPackages();
    }

    @PostMapping("/billing/packages")
    public ResponseEntity<BillingPackageResponse> createBillingPackage(
            @Valid @RequestBody BillingPackageRequest request) {
        BillingPackageResponse created = billingService.createPackage(request);
        return ResponseEntity.created(URI.create("/api/admin/billing/packages/" + created.id())).body(created);
    }

    @PutMapping("/billing/packages/{packageId}")
    public BillingPackageResponse updateBillingPackage(
            @PathVariable UUID packageId,
            @Valid @RequestBody BillingPackageRequest request) {
        return billingService.updatePackage(packageId, request);
    }

    @PutMapping("/billing/qr")
    public String updatePaymentQr(@Valid @RequestBody BillingSettingRequest request) {
        return billingService.updateQrImageUrl(request.qrImageUrl());
    }

    @GetMapping("/billing/qr")
    public String paymentQr() {
        return billingService.getQrImageUrl();
    }
}
