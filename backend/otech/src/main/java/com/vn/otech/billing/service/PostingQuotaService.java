package com.vn.otech.billing.service;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.billing.repository.ListingCreditGrantRepository;
import com.vn.otech.entity.User;
import com.vn.otech.market.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostingQuotaService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ListingCreditGrantRepository creditGrantRepository;
    private final BillingClock billingClock;
    private final int freeListingsPerDay;

    public PostingQuotaService(
            UserRepository userRepository,
            ProductRepository productRepository,
            ListingCreditGrantRepository creditGrantRepository,
            BillingClock billingClock,
            @Value("${app.monetization.free-listings-per-day:1}") int freeListingsPerDay) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.creditGrantRepository = creditGrantRepository;
        this.billingClock = billingClock;
        if (freeListingsPerDay < 1) {
            throw new IllegalArgumentException("The daily free listing limit must be at least one");
        }
        this.freeListingsPerDay = freeListingsPerDay;
    }

    @Transactional
    public void authorizeNewListing(User seller) {
        User lockedSeller = userRepository.findByIdForUpdate(seller.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));
        var now = billingClock.now();
        var today = billingClock.today();
        long publishedToday = productRepository.countBySeller_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                lockedSeller.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        if (publishedToday < freeListingsPerDay) {
            return;
        }

        var usableCredits = creditGrantRepository.findUsableForUpdate(lockedSeller.getId(), now);
        if (usableCredits.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "Daily free listing limit reached. Purchase a listing package to post another item.");
        }
        var creditGrant = usableCredits.getFirst();
        creditGrant.setCreditsRemaining(creditGrant.getCreditsRemaining() - 1);
        creditGrantRepository.save(creditGrant);
    }

    @Transactional(readOnly = true)
    public Usage getUsage(User user) {
        var today = billingClock.today();
        long publishedToday = productRepository.countBySeller_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                user.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        int freeListingsUsed = (int) Math.min(publishedToday, freeListingsPerDay);
        long availableCredits = creditGrantRepository.countUsableCredits(user.getId(), billingClock.now());
        return new Usage(freeListingsPerDay, freeListingsUsed, availableCredits);
    }

    public record Usage(int freeListingsPerDay, int freeListingsUsedToday, long availableListingCredits) {
    }
}
