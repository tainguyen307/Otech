package com.vn.otech.market.service;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.billing.service.BillingClock;
import com.vn.otech.chat.repository.ConversationParticipantRepository;
import com.vn.otech.chat.repository.ConversationRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Conversation;
import com.vn.otech.entity.ConversationParticipant;
import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.User;
import com.vn.otech.market.dto.ProductReviewRequest;
import com.vn.otech.market.dto.ProductReviewsResponse;
import com.vn.otech.market.dto.ProductTransactionResponse;
import com.vn.otech.market.entity.ProductReview;
import com.vn.otech.market.entity.ProductTransaction;
import com.vn.otech.market.entity.ProductTransactionStatus;
import com.vn.otech.market.repository.ProductRepository;
import com.vn.otech.market.repository.ProductReviewRepository;
import com.vn.otech.market.repository.ProductTransactionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductTransactionService {
    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;
    private final ProductTransactionRepository transactionRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final BillingClock billingClock;

    public ProductTransactionService(
            ProductRepository productRepository,
            ProductReviewRepository reviewRepository,
            ProductTransactionRepository transactionRepository,
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            UserRepository userRepository,
            BillingClock billingClock) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.transactionRepository = transactionRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.billingClock = billingClock;
    }

    public ProductTransactionResponse getTransaction(String email, UUID conversationId) {
        User user = findActiveUser(email);
        Conversation conversation = findConversation(conversationId);
        List<ConversationParticipant> participants = findParticipants(conversationId);
        requireParticipant(participants, user.getId());
        return transactionRepository.findByConversation_Id(conversationId)
                .map(transaction -> toResponse(transaction, user))
                .orElse(null);
    }

    @Transactional
    public ProductTransactionResponse confirmTransaction(String email, UUID conversationId) {
        User user = findActiveUser(email);
        Conversation conversation = findConversation(conversationId);
        if (conversation.getProduct() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transaction confirmation is only available in a product conversation");
        }
        List<ConversationParticipant> participants = findParticipants(conversationId);
        requireParticipant(participants, user.getId());
        Product product = productRepository.findWithSellerAndCategoryByIdForUpdate(conversation.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        User seller = product.getSeller();
        if (participants.size() != 2 || participants.stream().noneMatch(
                participant -> participant.getUser().getId().equals(seller.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The conversation does not match the listing's seller");
        }
        User buyer = participants.stream()
                .map(ConversationParticipant::getUser)
                .filter(participant -> !participant.getId().equals(seller.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Could not identify the buyer in this conversation"));
        ProductTransaction transaction = transactionRepository.findByConversation_Id(conversationId).orElse(null);
        if (transaction == null) {
            if (product.getStatus() != ProductStatus.SELLING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This listing is no longer available");
            }
            transaction = new ProductTransaction();
            transaction.setConversation(conversation);
            transaction.setProduct(product);
            transaction.setBuyer(buyer);
            transaction.setSeller(seller);
            transaction.setStatus(ProductTransactionStatus.PENDING);
        } else if (transaction.getStatus() == ProductTransactionStatus.COMPLETED) {
            return toResponse(transaction, user);
        } else if (product.getStatus() != ProductStatus.SELLING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This listing is no longer available");
        } else if (!transaction.getProduct().getId().equals(product.getId())
                || !transaction.getBuyer().getId().equals(buyer.getId())
                || !transaction.getSeller().getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The transaction participants no longer match the conversation");
        }

        if (user.getId().equals(buyer.getId())) {
            transaction.setBuyerConfirmed(true);
        } else {
            transaction.setSellerConfirmed(true);
        }
        if (transaction.isBuyerConfirmed() && transaction.isSellerConfirmed()) {
            if (product.getStatus() != ProductStatus.SELLING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This listing was completed in another transaction");
            }
            transaction.setStatus(ProductTransactionStatus.COMPLETED);
            transaction.setCompletedAt(billingClock.now());
            product.setStatus(ProductStatus.SOLD);
            productRepository.save(product);
        }
        return toResponse(transactionRepository.saveAndFlush(transaction), user);
    }

    @Transactional
    public ProductReviewsResponse createReview(
            String email, UUID conversationId, ProductReviewRequest request) {
        User reviewer = findActiveUser(email);
        ProductTransaction transaction = transactionRepository.findByConversationIdForUpdate(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Confirm the transaction before reviewing this product"));
        if (transaction.getStatus() != ProductTransactionStatus.COMPLETED
                || !transaction.getBuyer().getId().equals(reviewer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the buyer can review a completed transaction");
        }
        if (reviewRepository.findByTransaction_Id(transaction.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already reviewed this transaction");
        }
        ProductReview review = new ProductReview();
        review.setTransaction(transaction);
        review.setProduct(transaction.getProduct());
        review.setReviewer(reviewer);
        review.setRating(request.rating());
        review.setComment(clean(request.comment()));
        reviewRepository.saveAndFlush(review);
        return getProductReviews(transaction.getProduct().getId());
    }

    public ProductReviewsResponse getProductReviews(UUID productId) {
        Product product = productRepository.findWithSellerAndCategoryById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (product.getStatus() == ProductStatus.HIDDEN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        List<ProductReviewsResponse.Review> reviews =
                reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId).stream()
                        .map(review -> new ProductReviewsResponse.Review(
                                review.getId(),
                                review.getReviewer().getFullName(),
                                review.getRating(),
                                review.getComment(),
                                review.getCreatedAt()))
                        .toList();
        double averageRating = Math.round(reviewRepository.averageRating(productId) * 10.0) / 10.0;
        return new ProductReviewsResponse(averageRating, reviews.size(), reviews);
    }

    private ProductTransactionResponse toResponse(ProductTransaction transaction, User user) {
        boolean buyer = transaction.getBuyer().getId().equals(user.getId());
        boolean seller = transaction.getSeller().getId().equals(user.getId());
        if (!buyer && !seller) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not part of this transaction");
        }
        boolean currentUserConfirmed = buyer ? transaction.isBuyerConfirmed() : transaction.isSellerConfirmed();
        boolean otherPartyConfirmed = buyer ? transaction.isSellerConfirmed() : transaction.isBuyerConfirmed();
        return new ProductTransactionResponse(
                transaction.getId(),
                transaction.getProduct().getId(),
                transaction.getProduct().getTitle(),
                transaction.getStatus(),
                buyer,
                seller,
                currentUserConfirmed,
                otherPartyConfirmed,
                buyer && reviewRepository.findByTransaction_Id(transaction.getId()).isPresent(),
                transaction.getCompletedAt());
    }

    private Conversation findConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private List<ConversationParticipant> findParticipants(UUID conversationId) {
        return participantRepository.findByConversation_IdIn(List.of(conversationId));
    }

    private void requireParticipant(List<ConversationParticipant> participants, UUID userId) {
        if (participants.stream().noneMatch(participant -> participant.getUser().getId().equals(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not a participant in this conversation");
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

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
