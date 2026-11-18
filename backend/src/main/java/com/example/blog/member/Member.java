package com.example.blog.member;

import com.example.blog.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** member 테이블의 한 행. 로그인할 수 있는 계정이다. */
@Entity
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 해시된 비밀번호. 원문은 어디에도 저장하지 않는다. */
    @Column(nullable = false)
    private String password;

    // 기본값(ORDINAL)은 enum의 순서(0, 1)를 저장해서, 나중에 enum 순서를 바꾸면 권한이 뒤바뀐다.
    // STRING은 이름("ADMIN")을 그대로 저장한다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected Member() {
    }

    public Member(String username, String encodedPassword, Role role) {
        this.username = username;
        this.password = encodedPassword;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }
}
