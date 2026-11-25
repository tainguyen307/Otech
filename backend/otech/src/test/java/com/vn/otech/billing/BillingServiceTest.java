package com.vn.otech.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.billing.dto.BillingPackageRequest;
import com.vn.otech.billing.dto.BillingPaymentDecisionRequest;
import com.vn.otech.billing.dto.BillingPaymentRequestCreate;
import com.vn.otech.billing.entity.BillingPackage;
import com.vn.otech.billing.entity.BillingPackageType;
import com.vn.otech.billing.entity.BillingPaymentRequest;
import com.vn.otech.billing.entity.BillingSetting;
import com.vn.otech.billing.entity.PaymentRequestStatus;
import com.vn.otech.billing.repository.BillingPackageRepository;
import com.vn.otech.billing.repository.BillingPaymentRequestRepository;
import com.vn.otech.billing.repository.BillingSettingRepository;
import com.vn.otech.billing.repository.ListingCreditGrantRepository;
import com.vn.otech.billing.service.BillingClock;
import com.vn.otech.billing.service.BillingService;
import com.vn.otech.billing.service.PostingQuotaService;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.User;
import com.vn.otech.market.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    @Mock private BillingPackageRepository packageRepository;
    @Mock private BillingPaymentRequestRepository paymentRequestRepository;
    @Mock private BillingSettingRepository settingRepository;
    @Mock private ListingCreditGrantRepository creditGrantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostingQuotaService postingQuotaService;
    @Mock private BillingClock billingClock;
    @InjectMocks private BillingService billingService;

    @Test
    void createsPaymentRequestBeforeUserSubmitsTransferProof() {
        UUID packageId = UUID.randomUUID();
        BillingPackage billingPackage = listingPackage(packageId);
        BillingSetting setting = new BillingSetting();
        setting.setValue("https://example.test/qr.png");
        User user = activeUser("seller@example.com");
        when(userRepository.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(user));
        when(settingRepository.findById("payment_qr_image_url")).thenReturn(Optional.of(setting));
        when(packageRepository.findByIdForUpdate(packageId)).thenReturn(Optional.of(billingPackage));
        when(paymentRequestRepository.saveAndFlush(any(BillingPaymentRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = billingService.createPaymentRequest(
                "seller@example.com", new BillingPaymentRequestCreate(packageId, null, null, null));

        assertEquals(PaymentRequestStatus.PENDING, response.status());
        assertEquals(billingPackage.getPrice(), response.amount());
        assertNull(response.transferReference());
        assertNull(response.proofImageUrl());
    }

    @Test
    void doesNotApprovePaymentUntilProofHasBeenSubmitted() {
        UUID requestId = UUID.randomUUID();
        User payer = activeUser("seller@example.com");
        BillingPaymentRequest paymentRequest = new BillingPaymentRequest();
        paymentRequest.setUser(payer);
        paymentRequest.setBillingPackage(listingPackage(UUID.randomUUID()));
        paymentRequest.setStatus(PaymentRequestStatus.PENDING);
        when(paymentRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(paymentRequest));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                billingService.reviewPaymentRequest(
                        requestId,
                        new BillingPaymentDecisionRequest(PaymentRequestStatus.APPROVED, null),
                        "manager@example.com"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(userRepository, never()).findByEmailIgnoreCase("manager@example.com");
        verify(creditGrantRepository, never()).save(any());
    }

    @Test
    void doesNotChangePackageTermsWhilePaymentsArePending() {
        UUID packageId = UUID.randomUUID();
        when(packageRepository.findByIdForUpdate(packageId))
                .thenReturn(Optional.of(listingPackage(packageId)));
        when(paymentRequestRepository.countByBillingPackage_IdAndStatus(
                packageId, PaymentRequestStatus.PENDING)).thenReturn(1L);
        BillingPackageRequest update = new BillingPackageRequest(
                "POST_10", "10 listings", null, BillingPackageType.LISTING_CREDITS,
                BigDecimal.valueOf(5000), 10, 720, true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                billingService.updatePackage(packageId, update));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(packageRepository, never()).saveAndFlush(any());
    }

    private BillingPackage listingPackage(UUID id) {
        BillingPackage billingPackage = new BillingPackage();
        billingPackage.setId(id);
        billingPackage.setName("10 listings");
        billingPackage.setPackageType(BillingPackageType.LISTING_CREDITS);
        billingPackage.setPrice(BigDecimal.valueOf(5000));
        billingPackage.setListingCredits(10);
        billingPackage.setDurationHours(720);
        billingPackage.setActive(true);
        return billingPackage;
    }

    private User activeUser(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Test user");
        user.setEmail(email);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }
}
