package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter @Setter @NoArgsConstructor
public class Message {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id") private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id") private User sender;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(columnDefinition = "message_status") private MessageStatus status = MessageStatus.SENT;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
