package com.vn.otech.billing.service;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.billing.dto.BillingCatalogResponse;
import com.vn.otech.billing.dto.BillingPackageRequest;
import com.vn.otech.billing.dto.BillingPackageResponse;
import com.vn.otech.billing.dto.BillingPaymentDecisionRequest;
import com.vn.otech.billing.dto.BillingPaymentRequestCreate;
import com.vn.otech.billing.dto.BillingPaymentProofRequest;
import com.vn.otech.billing.dto.BillingPaymentResponse;
import com.vn.otech.billing.entity.BillingPackage;
import com.vn.otech.billing.entity.BillingPackageType;
import com.vn.otech.billing.entity.BillingPaymentRequest;
import com.vn.otech.billing.entity.BillingSetting;
import com.vn.otech.billing.entity.ListingCreditGrant;
import com.vn.otech.billing.entity.PaymentRequestStatus;
import com.vn.otech.billing.repository.BillingPackageRepository;
import com.vn.otech.billing.repository.BillingPaymentRequestRepository;
import com.vn.otech.billing.repository.BillingSettingRepository;
import com.vn.otech.billing.repository.ListingCreditGrantRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.User;
import com.vn.otech.market.repository.ProductRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class BillingService {
    private static final String PAYMENT_QR_SETTING = "payment_qr_image_url";

    private final BillingPackageRepository packageRepository;
    private final BillingPaymentRequestRepository paymentRequestRepository;
    private final BillingSettingRepository settingRepository;
    private final ListingCreditGrantRepository creditGrantRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PostingQuotaService postingQuotaService;
    private final BillingClock billingClock;

    public BillingService(
            BillingPackageRepository packageRepository,
            BillingPaymentRequestRepository paymentRequestRepository,
            BillingSettingRepository settingRepository,
            ListingCreditGrantRepository creditGrantRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PostingQuotaService postingQuotaService,
            BillingClock billingClock) {
        this.packageRepository = packageRepository;
        this.paymentRequestRepository = paymentRequestRepository;
        this.settingRepository = settingRepository;
        this.creditGrantRepository = creditGrantRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.postingQuotaService = postingQuotaService;
        this.billingClock = billingClock;
    }

    public BillingCatalogResponse getCatalog(String email) {
        User user = findActiveUser(email);
        PostingQuotaService.Usage usage = postingQuotaService.getUsage(user);
        return new BillingCatalogResponse(
                packageRepository.findByActiveTrueOrderByPriceAsc().stream().map(this::toPackageResponse).toList(),
                settingRepository.findById(PAYMENT_QR_SETTING).map(BillingSetting::getValue).orElse(null),
                usage.freeListingsPerDay(),
                usage.freeListingsUsedToday(),
                usage.availableListingCredits());
    }

    public List<BillingPaymentResponse> listMyPaymentRequests(String email) {
        User user = findActiveUser(email);
        return paymentRequestRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    @Transactional
    public BillingPaymentResponse updatePaymentProof(
            String email, UUID paymentRequestId, BillingPaymentProofRequest proof) {
        User user = findActiveUser(email);
        BillingPaymentRequest request = paymentRequestRepository.findByIdForUpdate(paymentRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment request not found"));
        if (!request.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot update another user's payment request");
        }
        if (request.getStatus() != PaymentRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment proof can only be updated while the request is pending");
        }
        String transferReference = clean(proof.transferReference());
        String proofImageUrl = clean(proof.proofImageUrl());
        if (transferReference == null && proofImageUrl == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide a bank transfer reference or upload a payment receipt");
        }
        request.setTransferReference(transferReference);
        request.setProofImageUrl(proofImageUrl);
        return toPaymentResponse(paymentRequestRepository.saveAndFlush(request));
    }

    @Transactional
    public BillingPaymentResponse createPaymentRequest(String email, BillingPaymentRequestCreate request) {
        User user = findActiveUser(email);
        requireQrConfigured();
        BillingPackage billingPackage = packageRepository.findByIdForUpdate(request.packageId())
                .filter(BillingPackage::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        Product product = null;
        if (billingPackage.getPackageType() == BillingPackageType.LISTING_CREDITS) {
            if (request.productId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A listing-credit package must not be linked to a product");
            }
        } else {
            if (request.productId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choose a listing to request a boost");
            }
            product = productRepository.findWithSellerAndCategoryById(request.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
            if (!product.getSeller().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can only boost your own listings");
            }
            if (product.getStatus() != ProductStatus.SELLING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Only active listings can be boosted");
            }
        }

        BillingPaymentRequest paymentRequest = new BillingPaymentRequest();
        paymentRequest.setUser(user);
        paymentRequest.setBillingPackage(billingPackage);
        paymentRequest.setProduct(product);
        paymentRequest.setPaymentCode("OT" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase());
        paymentRequest.setAmount(billingPackage.getPrice());
        paymentRequest.setTransferReference(clean(request.transferReference()));
        paymentRequest.setProofImageUrl(clean(request.proofImageUrl()));
        paymentRequest.setStatus(PaymentRequestStatus.PENDING);
        return toPaymentResponse(paymentRequestRepository.saveAndFlush(paymentRequest));
    }

    public List<BillingPackageResponse> listAllPackages() {
        return packageRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toPackageResponse)
                .toList();
    }

    @Transactional
    public BillingPackageResponse createPackage(BillingPackageRequest request) {
        validatePackageRequest(request);
        String code = request.code().trim().toUpperCase();
        if (packageRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package code already exists");
        }
        BillingPackage billingPackage = new BillingPackage();
        applyPackageRequest(billingPackage, request, code);
        return toPackageResponse(packageRepository.saveAndFlush(billingPackage));
    }

    @Transactional
    public BillingPackageResponse updatePackage(UUID packageId, BillingPackageRequest request) {
        validatePackageRequest(request);
        BillingPackage billingPackage = packageRepository.findByIdForUpdate(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        if (paymentRequestRepository.countByBillingPackage_IdAndStatus(
                packageId, PaymentRequestStatus.PENDING) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Resolve pending payments before changing this package");
        }
        String code = request.code().trim().toUpperCase();
        packageRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(packageId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Package code already exists");
                });
        applyPackageRequest(billingPackage, request, code);
        return toPackageResponse(packageRepository.saveAndFlush(billingPackage));
    }

    @Transactional
    public String updateQrImageUrl(String imageUrl) {
        BillingSetting setting = settingRepository.findById(PAYMENT_QR_SETTING).orElseGet(BillingSetting::new);
        setting.setKey(PAYMENT_QR_SETTING);
        setting.setValue(clean(imageUrl));
        settingRepository.save(setting);
        return setting.getValue();
    }

    public String getQrImageUrl() {
        return settingRepository.findById(PAYMENT_QR_SETTING).map(BillingSetting::getValue).orElse(null);
    }

    public List<BillingPaymentResponse> listPaymentRequests(PaymentRequestStatus status) {
        return paymentRequestRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    @Transactional
    public BillingPaymentResponse reviewPaymentRequest(
            UUID paymentRequestId,
            BillingPaymentDecisionRequest decision,
            String reviewerEmail) {
        if (decision.status() != PaymentRequestStatus.APPROVED
                && decision.status() != PaymentRequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A payment request can only be approved or rejected");
        }
        BillingPaymentRequest request = paymentRequestRepository.findByIdForUpdate(paymentRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment request not found"));
        if (request.getStatus() != PaymentRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment request has already been reviewed");
        }
        if (decision.status() == PaymentRequestStatus.APPROVED
                && clean(request.getTransferReference()) == null
                && clean(request.getProofImageUrl()) == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment proof is required before approving this request");
        }
        User reviewer = findActiveUser(reviewerEmail);
        if (decision.status() == PaymentRequestStatus.APPROVED) {
            BillingPackage billingPackage = packageRepository.findByIdForUpdate(
                            request.getBillingPackage().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
            request.setBillingPackage(billingPackage);
            grantPackage(request);
        }
        request.setStatus(decision.status());
        request.setReviewNote(clean(decision.reviewNote()));
        request.setReviewedBy(reviewer);
        request.setReviewedAt(billingClock.now());
        return toPaymentResponse(paymentRequestRepository.saveAndFlush(request));
    }

    private void grantPackage(BillingPaymentRequest request) {
        BillingPackage billingPackage = request.getBillingPackage();
        if (billingPackage.getPackageType() == BillingPackageType.LISTING_CREDITS) {
            ListingCreditGrant grant = new ListingCreditGrant();
            grant.setUser(request.getUser());
            grant.setPaymentRequest(request);
            grant.setCreditsGranted(billingPackage.getListingCredits());
            grant.setCreditsRemaining(billingPackage.getListingCredits());
            grant.setValidUntil(billingClock.now().plus(billingPackage.getDurationHours(), ChronoUnit.HOURS));
            creditGrantRepository.save(grant);
            return;
        }

        Product product = productRepository.findWithSellerAndCategoryByIdForUpdate(request.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (!product.getSeller().getId().equals(request.getUser().getId())
                || product.getStatus() != ProductStatus.SELLING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Listing is no longer available for a boost");
        }
        var current = billingClock.now();
        var startsAt = product.getBoostedUntil() != null && product.getBoostedUntil().isAfter(current)
                ? product.getBoostedUntil()
                : current;
        product.setBoostedUntil(startsAt.plus(billingPackage.getDurationHours(), ChronoUnit.HOURS));
        productRepository.save(product);
    }

    private void applyPackageRequest(BillingPackage billingPackage, BillingPackageRequest request, String code) {
        billingPackage.setCode(code);
        billingPackage.setName(request.name().trim());
        billingPackage.setDescription(clean(request.description()));
        billingPackage.setPackageType(request.packageType());
        billingPackage.setPrice(request.price());
        billingPackage.setListingCredits(request.packageType() == BillingPackageType.LISTING_CREDITS
                ? request.listingCredits()
                : null);
        billingPackage.setDurationHours(request.durationHours());
        billingPackage.setActive(request.active());
    }

    private void validatePackageRequest(BillingPackageRequest request) {
        if (request.packageType() == BillingPackageType.LISTING_CREDITS
                && (request.listingCredits() == null || request.listingCredits() < 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Listing-credit packages must contain at least one credit");
        }
        if (request.packageType() == BillingPackageType.BOOST && request.listingCredits() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Boost packages cannot contain listing credits");
        }
    }

    private void requireQrConfigured() {
        if (getQrImageUrl() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The administrator has not configured the payment QR code");
        }
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is not active");
        }
        return user;
    }

    private BillingPackageResponse toPackageResponse(BillingPackage billingPackage) {
        return new BillingPackageResponse(
                billingPackage.getId(),
                billingPackage.getCode(),
                billingPackage.getName(),
                billingPackage.getDescription(),
                billingPackage.getPackageType(),
                billingPackage.getPrice(),
                billingPackage.getListingCredits(),
                billingPackage.getDurationHours(),
                billingPackage.isActive());
    }

    private BillingPaymentResponse toPaymentResponse(BillingPaymentRequest request) {
        return new BillingPaymentResponse(
                request.getId(),
                request.getBillingPackage().getName(),
                request.getBillingPackage().getPackageType().name(),
                request.getProduct() == null ? null : request.getProduct().getTitle(),
                request.getPaymentCode(),
                request.getAmount(),
                request.getTransferReference(),
                request.getProofImageUrl(),
                request.getStatus(),
                request.getReviewNote(),
                request.getUser().getFullName(),
                request.getUser().getEmail(),
                request.getCreatedAt(),
                request.getReviewedAt());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
