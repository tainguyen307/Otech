package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "group_members")
@Getter @Setter @NoArgsConstructor
public class GroupMember {
    @EmbeddedId private GroupMemberId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("groupId") @JoinColumn(name = "group_id") private Group group;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("userId") @JoinColumn(name = "user_id") private User user;
    @Column(length = 50) private String role = "MEMBER";
}
