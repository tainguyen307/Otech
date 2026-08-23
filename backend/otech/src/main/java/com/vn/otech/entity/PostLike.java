package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_likes")
@Getter @Setter @NoArgsConstructor
public class PostLike {
    @EmbeddedId private PostLikeId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("postId") @JoinColumn(name = "post_id") private Post post;
}
