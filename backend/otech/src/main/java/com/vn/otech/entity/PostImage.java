package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "post_images")
@Getter @Setter @NoArgsConstructor
public class PostImage {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false) private Post post;
    @Column(name = "image_url", nullable = false) private String imageUrl;
}
