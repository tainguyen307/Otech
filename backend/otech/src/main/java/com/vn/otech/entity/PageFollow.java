package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "page_follows")
@Getter @Setter @NoArgsConstructor
public class PageFollow {
    @EmbeddedId private PageFollowId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("pageId") @JoinColumn(name = "page_id") private Page page;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id") private User user;
}
