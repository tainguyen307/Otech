package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "group_post_images")
@Getter @Setter @NoArgsConstructor
public class GroupPostImage {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "group_post_id") private GroupPost groupPost;
    @Column(name = "image_url", nullable = false) private String imageUrl;
}
