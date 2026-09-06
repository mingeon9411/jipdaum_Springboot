package com.jipdaum_spring.domain.inquiry;

import com.jipdaum_spring.domain.jipdaumuser.JipdaumUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "JIPDAUM_INQUIRY")
@Getter
@Setter
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private JipdaumUser user;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "answer")
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
