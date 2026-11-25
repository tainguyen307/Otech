package com.vn.otech.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.billing.entity.ListingCreditGrant;
import com.vn.otech.billing.repository.ListingCreditGrantRepository;
import com.vn.otech.billing.service.BillingClock;
import com.vn.otech.billing.service.PostingQuotaService;
import com.vn.otech.entity.User;
import com.vn.otech.market.repository.ProductRepository;
import java.time.LocalDate;
import java.util.List;
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
class PostingQuotaServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ListingCreditGrantRepository creditGrantRepository;
    @Mock private BillingClock billingClock;

    private PostingQuotaService createService(int freeListingsPerDay) {
        return new PostingQuotaService(
                userRepository, productRepository, creditGrantRepository, billingClock, freeListingsPerDay);
    }

    @Test
    void allowsTheDailyFreeListingWithoutSpendingCredits() {
        User user = user();
        LocalDate today = LocalDate.of(2026, 11, 24);
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(billingClock.now()).thenReturn(today.atTime(12, 0));
        when(billingClock.today()).thenReturn(today);
        when(productRepository.countBySeller_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                user.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(0L);

        createService(1).authorizeNewListing(user);

        verify(creditGrantRepository, never()).findUsableForUpdate(any(), any());
    }

    @Test
    void requiresPaymentWhenDailyFreeLimitIsReachedWithoutCredits() {
        User user = user();
        LocalDate today = LocalDate.of(2026, 11, 24);
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(billingClock.now()).thenReturn(today.atTime(12, 0));
        when(billingClock.today()).thenReturn(today);
        when(productRepository.countBySeller_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                user.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(1L);
        when(creditGrantRepository.findUsableForUpdate(user.getId(), today.atTime(12, 0))).thenReturn(List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                createService(1).authorizeNewListing(user));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, error.getStatusCode());
    }

    @Test
    void consumesOneCreditFromTheFirstUsableGrant() {
        User user = user();
        LocalDate today = LocalDate.of(2026, 11, 24);
        ListingCreditGrant firstGrant = new ListingCreditGrant();
        firstGrant.setCreditsRemaining(3);
        ListingCreditGrant laterGrant = new ListingCreditGrant();
        laterGrant.setCreditsRemaining(5);
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(billingClock.now()).thenReturn(today.atTime(12, 0));
        when(billingClock.today()).thenReturn(today);
        when(productRepository.countBySeller_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                user.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay())).thenReturn(1L);
        when(creditGrantRepository.findUsableForUpdate(user.getId(), today.atTime(12, 0)))
                .thenReturn(List.of(firstGrant, laterGrant));

        createService(1).authorizeNewListing(user);

        assertEquals(2, firstGrant.getCreditsRemaining());
        assertEquals(5, laterGrant.getCreditsRemaining());
        verify(creditGrantRepository).save(firstGrant);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }
}
