package com.vn.otech.chat.repository;

import com.vn.otech.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    @EntityGraph(attributePaths = {"product", "product.seller"})
    @Query("""
            select c from Conversation c
            where ((:productId is null and c.product is null) or c.product.id = :productId)
              and (select count(cp.user.id) from ConversationParticipant cp
                   where cp.conversation = c and cp.user.id in (:firstUserId, :secondUserId)) = 2
              and (select count(cp.user.id) from ConversationParticipant cp
                   where cp.conversation = c) = 2
            """)
    Optional<Conversation> findDirectConversation(
            @Param("productId") UUID productId,
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId);
}
