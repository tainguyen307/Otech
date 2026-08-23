package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "page_members")
@Getter @Setter @NoArgsConstructor
public class PageMember {
    @EmbeddedId private PageMemberId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("pageId") @JoinColumn(name = "page_id") private Page page;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id") private User user;
    @Column(length = 50) private String role = "MEMBER";
}
