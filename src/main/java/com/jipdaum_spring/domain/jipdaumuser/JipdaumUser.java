package com.jipdaum_spring.domain.jipdaumuser;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "JIPDAUM_USER")
@Getter
public class JipdaumUser {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "username")
    private String username;
}
