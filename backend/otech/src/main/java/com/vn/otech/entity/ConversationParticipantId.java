package com.vn.otech.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@EqualsAndHashCode
public class ConversationParticipantId implements Serializable {
    private UUID conversationId;
    private UUID userId;

    public ConversationParticipantId() {
    }

    public ConversationParticipantId(UUID conversationId, UUID userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }
}
