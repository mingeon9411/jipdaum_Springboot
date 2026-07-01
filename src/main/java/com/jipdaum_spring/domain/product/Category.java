package com.jipdaum_spring.domain.product;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "JIPDAUM_CATEGORY")
@Getter
public class Category {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;
}
