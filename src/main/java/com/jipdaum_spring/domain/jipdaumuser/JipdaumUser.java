package com.jipdaum_spring.domain.jipdaumuser;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @Setter
    @Column(name = "password")
    private String password;

    @Setter
    @Column(name = "is_email_verified")
    private Boolean emailVerified;

    @Setter
    @Column(name = "is_active")
    private Boolean active;

    // 아이디/비밀번호 찾기 본인확인용. 기존 회원은 NULL(미설정) — 로그인 시 설정 안내를 띄우는 기준.
    // security_answer는 평문이 아니라 BCrypt 해시로 저장한다(UserAuthService 참고).
    @Setter
    @Column(name = "security_question")
    private String securityQuestion;

    @Setter
    @Column(name = "security_answer")
    private String securityAnswer;
}
