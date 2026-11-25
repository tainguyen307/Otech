package com.vn.otech.market.repository;

import com.vn.otech.market.entity.ProductTransaction;
import com.vn.otech.market.entity.ProductTransactionStatus;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductTransactionRepository extends JpaRepository<ProductTransaction, UUID> {
    @EntityGraph(attributePaths = {"product", "buyer", "seller", "conversation"})
    Optional<ProductTransaction> findByConversation_Id(UUID conversationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select transaction from ProductTransaction transaction where transaction.conversation.id = :conversationId")
    Optional<ProductTransaction> findByConversationIdForUpdate(@Param("conversationId") UUID conversationId);

    boolean existsByProduct_IdAndStatus(UUID productId, ProductTransactionStatus status);
}
