package com.vn.otech.chat.repository;

import com.vn.otech.entity.Message;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    @Query("""
            select m from Message m
            where m.conversation.id in :conversationIds
              and not exists (
                  select newer.id from Message newer
                  where newer.conversation = m.conversation
                    and (newer.createdAt > m.createdAt
                         or (newer.createdAt = m.createdAt and newer.id > m.id))
              )
            """)
    List<Message> findLatestByConversationIds(@Param("conversationIds") Collection<UUID> conversationIds);

    @Query(value = """
            select conversation_id, count(id)
            from messages
            where conversation_id in (:conversationIds)
              and sender_id <> :userId
              and status <> 'READ'
            group by conversation_id
            """, nativeQuery = true)
    List<Object[]> countUnreadByConversationIds(
            @Param("conversationIds") Collection<UUID> conversationIds,
            @Param("userId") UUID userId);

    @Query("""
            select m from Message m
            where m.conversation.id = :conversationId
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findLatestByConversationId(
            @Param("conversationId") UUID conversationId,
            Pageable pageable);

    @Query("""
            select m from Message m
            where m.conversation.id = :conversationId
              and (m.createdAt < :beforeTime
                   or (m.createdAt = :beforeTime and m.id < :beforeId))
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findLatestBefore(
            @Param("conversationId") UUID conversationId,
            @Param("beforeTime") LocalDateTime beforeTime,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);

    Optional<Message> findBySender_IdAndClientMessageId(UUID senderId, UUID clientMessageId);

    @Modifying
    @Query(value = """
            update messages
            set status = 'READ'
            where conversation_id = :conversationId
              and sender_id <> :userId
              and status <> 'READ'
            """, nativeQuery = true)
    int markConversationRead(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
