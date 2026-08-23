package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_follows")
@Getter @Setter @NoArgsConstructor
public class UserFollow {
    @EmbeddedId private UserFollowId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("followerId") @JoinColumn(name = "follower_id") private User follower;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("followingId") @JoinColumn(name = "following_id") private User following;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
