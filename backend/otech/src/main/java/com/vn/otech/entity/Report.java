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
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor
public class Report {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reporter_id") private User reporter;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(name = "target_type", nullable = false, columnDefinition = "report_target_type") private ReportTargetType targetType;
    @Column(name = "target_id", nullable = false) private UUID targetId;
    @Column(nullable = false, columnDefinition = "TEXT") private String reason;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(columnDefinition = "report_status") private ReportStatus status = ReportStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "handled_by") private User handledBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
