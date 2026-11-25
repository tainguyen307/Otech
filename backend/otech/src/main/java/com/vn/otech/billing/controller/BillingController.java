package com.vn.otech.billing.controller;

import com.vn.otech.billing.dto.BillingCatalogResponse;
import com.vn.otech.billing.dto.BillingPaymentRequestCreate;
import com.vn.otech.billing.dto.BillingPaymentProofRequest;
import com.vn.otech.billing.dto.BillingPaymentResponse;
import com.vn.otech.billing.service.BillingService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/catalog")
    public BillingCatalogResponse catalog(@AuthenticationPrincipal UserDetails principal) {
        return billingService.getCatalog(principal.getUsername());
    }

    @GetMapping("/payment-requests")
    public List<BillingPaymentResponse> myPaymentRequests(@AuthenticationPrincipal UserDetails principal) {
        return billingService.listMyPaymentRequests(principal.getUsername());
    }

    @PostMapping("/payment-requests")
    public ResponseEntity<BillingPaymentResponse> createPaymentRequest(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody BillingPaymentRequestCreate request) {
        BillingPaymentResponse created = billingService.createPaymentRequest(principal.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/billing/payment-requests/" + created.id())).body(created);
    }

    @PatchMapping("/payment-requests/{paymentRequestId}/proof")
    public BillingPaymentResponse updatePaymentProof(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable java.util.UUID paymentRequestId,
            @Valid @RequestBody BillingPaymentProofRequest request) {
        return billingService.updatePaymentProof(principal.getUsername(), paymentRequestId, request);
    }
}
