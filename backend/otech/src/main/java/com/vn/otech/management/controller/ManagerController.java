package com.vn.otech.management.controller;

import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.ReportStatus;
import com.vn.otech.entity.RoleName;
import com.vn.otech.billing.dto.BillingPaymentDecisionRequest;
import com.vn.otech.billing.dto.BillingPaymentResponse;
import com.vn.otech.billing.entity.PaymentRequestStatus;
import com.vn.otech.billing.service.BillingService;
import com.vn.otech.management.dto.AccountStatusRequest;
import com.vn.otech.management.dto.ContentVisibilityRequest;
import com.vn.otech.management.dto.ManagementDashboardSummary;
import com.vn.otech.management.dto.ManagementPage;
import com.vn.otech.management.dto.ManagedGroupResponse;
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
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/manager")
public class ManagerController {
    private final ManagementService managementService;
    private final BillingService billingService;

    public ManagerController(ManagementService managementService, BillingService billingService) {
        this.managementService = managementService;
        this.billingService = billingService;
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

    @GetMapping("/groups")
    public ManagementPage<ManagedGroupResponse> groups(
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return managementService.listGroups(search, hidden, page, size);
    }

    @PatchMapping("/groups/{groupId}/visibility")
    public ManagedGroupResponse updateGroupVisibility(
            @PathVariable UUID groupId, @Valid @RequestBody ContentVisibilityRequest request) {
        return managementService.updateGroupVisibility(groupId, request.hidden());
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

    @GetMapping("/billing/payment-requests")
    public List<BillingPaymentResponse> paymentRequests(
            @RequestParam(defaultValue = "PENDING") PaymentRequestStatus status) {
        return billingService.listPaymentRequests(status);
    }

    @PatchMapping("/billing/payment-requests/{paymentRequestId}")
    public BillingPaymentResponse reviewPaymentRequest(
            @PathVariable UUID paymentRequestId,
            @Valid @RequestBody BillingPaymentDecisionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return billingService.reviewPaymentRequest(paymentRequestId, request, principal.getUsername());
    }
}
