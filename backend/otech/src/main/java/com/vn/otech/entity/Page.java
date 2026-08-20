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
@Table(name = "pages")
@Getter @Setter @NoArgsConstructor
public class Page {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") private User owner;
    @Column(nullable = false, length = 255) private String name;
    @Column(name = "avatar_url") private String avatarUrl;
    @Column(name = "cover_url") private String coverUrl;
    private String description;
    @Column(name = "contact_info") private String contactInfo;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Column(columnDefinition = "page_visibility") private PageVisibility visibility = PageVisibility.PUBLIC;
    @Column(name = "is_hidden") private boolean hidden;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
