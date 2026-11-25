package com.vn.otech.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.chat.repository.ConversationParticipantRepository;
import com.vn.otech.chat.repository.ConversationRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Conversation;
import com.vn.otech.entity.ConversationParticipant;
import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.User;
import com.vn.otech.billing.service.BillingClock;
import com.vn.otech.market.dto.ProductReviewRequest;
import com.vn.otech.market.entity.ProductReview;
import com.vn.otech.market.entity.ProductTransaction;
import com.vn.otech.market.entity.ProductTransactionStatus;
import com.vn.otech.market.repository.ProductRepository;
import com.vn.otech.market.repository.ProductReviewRepository;
import com.vn.otech.market.repository.ProductTransactionRepository;
import com.vn.otech.market.service.ProductTransactionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductTransactionServiceTest {
    @Mock private ProductRepository productRepository;
    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductTransactionRepository transactionRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;
    @Mock private BillingClock billingClock;
    @InjectMocks private ProductTransactionService transactionService;

    @Test
    void marksListingSoldOnlyAfterBothConversationParticipantsConfirm() {
        UUID productId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User buyer = user("buyer@example.com");
        User seller = user("seller@example.com");
        Product product = new Product();
        product.setId(productId);
        product.setSeller(seller);
        product.setStatus(ProductStatus.SELLING);
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setProduct(product);
        List<ConversationParticipant> participants = List.of(
                participant(buyer), participant(seller));
        AtomicReference<ProductTransaction> persistedTransaction = new AtomicReference<>();
        LocalDateTime completedAt = LocalDateTime.of(2026, 11, 24, 12, 0);

        when(userRepository.findByEmailIgnoreCase(buyer.getEmail())).thenReturn(Optional.of(buyer));
        when(userRepository.findByEmailIgnoreCase(seller.getEmail())).thenReturn(Optional.of(seller));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversation_IdIn(List.of(conversationId))).thenReturn(participants);
        when(productRepository.findWithSellerAndCategoryByIdForUpdate(productId)).thenReturn(Optional.of(product));
        when(transactionRepository.findByConversation_Id(conversationId))
                .thenAnswer(invocation -> Optional.ofNullable(persistedTransaction.get()));
        when(transactionRepository.saveAndFlush(any(ProductTransaction.class))).thenAnswer(invocation -> {
            ProductTransaction saved = invocation.getArgument(0);
            persistedTransaction.set(saved);
            return saved;
        });
        when(reviewRepository.findByTransaction_Id(any())).thenReturn(Optional.empty());
        when(billingClock.now()).thenReturn(completedAt);

        var buyerConfirmation = transactionService.confirmTransaction(buyer.getEmail(), conversationId);
        assertEquals(ProductTransactionStatus.PENDING, buyerConfirmation.status());
        assertTrue(buyerConfirmation.currentUserConfirmed());
        assertFalse(buyerConfirmation.otherPartyConfirmed());
        assertEquals(ProductStatus.SELLING, product.getStatus());

        var sellerConfirmation = transactionService.confirmTransaction(seller.getEmail(), conversationId);
        assertEquals(ProductTransactionStatus.COMPLETED, sellerConfirmation.status());
        assertTrue(sellerConfirmation.currentUserConfirmed());
        assertTrue(sellerConfirmation.otherPartyConfirmed());
        assertEquals(ProductStatus.SOLD, product.getStatus());
        assertEquals(completedAt, sellerConfirmation.completedAt());
        verify(productRepository).save(product);
    }

    @Test
    void allowsOnlyTheCompletedTransactionBuyerToSubmitAProductReview() {
        UUID productId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User buyer = user("buyer@example.com");
        User seller = user("seller@example.com");
        Product product = new Product();
        product.setId(productId);
        product.setSeller(seller);
        product.setStatus(ProductStatus.SOLD);
        ProductTransaction transaction = new ProductTransaction();
        transaction.setId(transactionId);
        transaction.setProduct(product);
        transaction.setBuyer(buyer);
        transaction.setSeller(seller);
        transaction.setStatus(ProductTransactionStatus.COMPLETED);
        AtomicReference<ProductReview> savedReview = new AtomicReference<>();

        when(userRepository.findByEmailIgnoreCase(buyer.getEmail())).thenReturn(Optional.of(buyer));
        when(transactionRepository.findByConversationIdForUpdate(conversationId))
                .thenReturn(Optional.of(transaction));
        when(reviewRepository.findByTransaction_Id(transactionId)).thenReturn(Optional.empty());
        when(reviewRepository.saveAndFlush(any(ProductReview.class))).thenAnswer(invocation -> {
            ProductReview review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            review.setCreatedAt(LocalDateTime.of(2026, 11, 24, 12, 0));
            savedReview.set(review);
            return review;
        });
        when(productRepository.findWithSellerAndCategoryById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId))
                .thenAnswer(invocation -> List.of(savedReview.get()));
        when(reviewRepository.averageRating(productId)).thenReturn(4.0);

        var reviews = transactionService.createReview(
                buyer.getEmail(), conversationId, new ProductReviewRequest(4, "As described"));

        assertEquals(1, reviews.reviewCount());
        assertEquals(4.0, reviews.averageRating());
        assertEquals(4, reviews.reviews().getFirst().rating());
        assertEquals("As described", reviews.reviews().getFirst().comment());
    }

    @Test
    void sellersCannotReviewTheBuyersCompletedTransaction() {
        UUID conversationId = UUID.randomUUID();
        User buyer = user("buyer@example.com");
        User seller = user("seller@example.com");
        ProductTransaction transaction = new ProductTransaction();
        transaction.setBuyer(buyer);
        transaction.setSeller(seller);
        transaction.setStatus(ProductTransactionStatus.COMPLETED);
        when(userRepository.findByEmailIgnoreCase(seller.getEmail())).thenReturn(Optional.of(seller));
        when(transactionRepository.findByConversationIdForUpdate(conversationId))
                .thenReturn(Optional.of(transaction));

        org.springframework.web.server.ResponseStatusException error =
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () -> transactionService.createReview(
                                seller.getEmail(), conversationId, new ProductReviewRequest(5, null)));

        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any(ProductReview.class));
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName(email.substring(0, email.indexOf('@')));
        user.setEmail(email);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    private ConversationParticipant participant(User user) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setUser(user);
        return participant;
    }
}
