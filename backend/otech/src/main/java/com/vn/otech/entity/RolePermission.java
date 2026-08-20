package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_permissions")
@Getter @Setter @NoArgsConstructor
public class RolePermission {
    @EmbeddedId private RolePermissionId id;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("roleId") @JoinColumn(name = "role_id") private Role role;
    @ManyToOne(fetch = FetchType.LAZY) @MapsId("permissionId") @JoinColumn(name = "permission_id") private Permission permission;
}
