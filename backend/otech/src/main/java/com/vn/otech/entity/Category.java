package com.vn.otech.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    private String description;
}
