package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_participants")
@Getter @Setter @NoArgsConstructor
public class ConversationParticipant {
    @EmbeddedId private ConversationParticipantId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("conversationId") @JoinColumn(name = "conversation_id") private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id") private User user;
}
