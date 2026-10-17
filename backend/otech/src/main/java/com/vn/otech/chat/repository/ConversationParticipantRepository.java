package com.vn.otech.chat.repository;

import com.vn.otech.entity.ConversationParticipant;
import com.vn.otech.entity.ConversationParticipantId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, ConversationParticipantId> {
    @EntityGraph(attributePaths = {"conversation", "conversation.product", "conversation.product.seller", "user"})
    List<ConversationParticipant> findByUser_Id(UUID userId);

    @EntityGraph(attributePaths = {"user"})
    List<ConversationParticipant> findByConversation_IdIn(Collection<UUID> conversationIds);

    boolean existsByConversation_IdAndUser_Id(UUID conversationId, UUID userId);
}
