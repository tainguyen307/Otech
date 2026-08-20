package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "page_post_images")
@Getter @Setter @NoArgsConstructor
public class PagePostImage {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "page_post_id") private PagePost pagePost;
    @Column(name = "image_url", nullable = false) private String imageUrl;
}
