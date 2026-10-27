package com.vn.otech.management.controller;

import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.ReportStatus;
import com.vn.otech.entity.RoleName;
import com.vn.otech.management.dto.AccountStatusRequest;
import com.vn.otech.management.dto.ContentVisibilityRequest;
import com.vn.otech.management.dto.ManagementDashboardSummary;
import com.vn.otech.management.dto.ManagementPage;
import com.vn.otech.management.dto.ManagedPageResponse;
import com.vn.otech.management.dto.ManagedPostResponse;
import com.vn.otech.management.dto.ManagedProductResponse;
import com.vn.otech.management.dto.ManagedReportResponse;
import com.vn.otech.management.dto.ManagedUserResponse;
import com.vn.otech.management.dto.ReportDecisionRequest;
import com.vn.otech.management.service.ManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/manager")
public class ManagerController {
    private final ManagementService managementService;

    public ManagerController(ManagementService managementService) {
        this.managementService = managementService;
    }

    @GetMapping("/dashboard")
    public ManagementDashboardSummary dashboard() {
        return managementService.getSummary();
    }

    @GetMapping("/users")
    public ManagementPage<ManagedUserResponse> users(
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return managementService.listUsers(search, role, status, page, size);
    }

    @PatchMapping("/users/{userId}/status")
    public ManagedUserResponse updateUserStatus(
            @PathVariable UUID userId, @Valid @RequestBody AccountStatusRequest request) {
        return managementService.updateUserStatus(userId, request.status());
    }

    @GetMapping("/products")
    public ManagementPage<ManagedProductResponse> products(
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return managementService.listProducts(search, status, page, size);
    }

    @PatchMapping("/products/{productId}/visibility")
    public ManagedProductResponse updateProductVisibility(
            @PathVariable UUID productId, @Valid @RequestBody ContentVisibilityRequest request) {
        return managementService.updateProductVisibility(productId, request.hidden());
    }

    @GetMapping("/posts")
    public ManagementPage<ManagedPostResponse> posts(
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return managementService.listPosts(search, hidden, page, size);
    }

    @PatchMapping("/posts/{postId}/visibility")
    public ManagedPostResponse updatePostVisibility(
            @PathVariable UUID postId, @Valid @RequestBody ContentVisibilityRequest request) {
        return managementService.updatePostVisibility(postId, request.hidden());
    }

    @GetMapping("/pages")
    public ManagementPage<ManagedPageResponse> pages(
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return managementService.listPages(search, hidden, page, size);
    }

    @PatchMapping("/pages/{pageId}/visibility")
    public ManagedPageResponse updatePageVisibility(
            @PathVariable UUID pageId, @Valid @RequestBody ContentVisibilityRequest request) {
        return managementService.updatePageVisibility(pageId, request.hidden());
    }

    @GetMapping("/reports")
    public ManagementPage<ManagedReportResponse> reports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return managementService.listReports(status, page, size);
    }

    @PatchMapping("/reports/{reportId}")
    public ManagedReportResponse decideReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportDecisionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return managementService.decideReport(reportId, request.status(), principal.getUsername());
    }
}
