package com.example.blog.comment;

import com.example.blog.common.BaseTimeEntity;
import com.example.blog.post.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * comment 테이블의 한 행. 회원가입이 없으므로 방문자는 이름과 비밀번호를 적고 댓글을 쓰며,
 * 그 비밀번호로 자기 댓글을 지운다. 관리자의 댓글은 로그인으로 확인하므로 비밀번호가 없다.
 */
@Entity
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글 여럿이 글 하나에 달린다(다대일). 댓글을 읽을 때 글까지 매번 조회할 필요는 없으므로 지연 로딩.
    // @OnDelete: 글이 지워질 때 댓글은 DB가 지운다(on delete cascade)는 사실을 Hibernate에 알려준다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @Column(nullable = false, length = 20)
    private String authorName;

    /** 해시된 비밀번호. 관리자의 댓글은 null. */
    @Column
    private String password;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private boolean byAdmin;

    protected Comment() {
    }

    private Comment(Post post, String authorName, String encodedPassword, String content, boolean byAdmin) {
        this.post = post;
        this.authorName = authorName;
        this.password = encodedPassword;
        this.content = content;
        this.byAdmin = byAdmin;
    }

    // 생성자 대신 이름 있는 메서드를 둔다. 두 경우에 필요한 값이 다르다는 것이 호출하는 쪽에서 드러난다.

    public static Comment byGuest(Post post, String authorName, String encodedPassword, String content) {
        return new Comment(post, authorName, encodedPassword, content, false);
    }

    public static Comment byAdmin(Post post, String adminName, String content) {
        return new Comment(post, adminName, null, content, true);
    }

    public Long getId() {
        return id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getPassword() {
        return password;
    }

    public String getContent() {
        return content;
    }

    public boolean isByAdmin() {
        return byAdmin;
    }
}
