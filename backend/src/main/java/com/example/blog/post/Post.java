package com.example.blog.post;

import com.example.blog.common.BaseTimeEntity;
import com.example.blog.tag.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** post 테이블의 한 행에 대응하는 엔티티. */
@Entity
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /**
     * 이 글에 붙은 태그들. 연결 테이블 post_tag로 이어진다.
     * 기본이 지연 로딩(LAZY)이라, 글을 조회할 때는 태그를 읽지 않고 실제로 꺼내 쓸 때 읽는다.
     * List가 아니라 Set인 이유: List로 두면 태그 하나만 바뀌어도 Hibernate가 연결 행을
     * 전부 지우고 다시 넣는다. Set은 바뀐 것만 반영한다.
     */
    @ManyToMany
    @JoinTable(
            name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    /** JPA가 DB에서 읽어 객체를 만들 때 쓰는 생성자. 코드에서 직접 부르지 않는다. */
    protected Post() {
    }

    public Post(String title, String content, Set<Tag> tags) {
        this.title = title;
        this.content = content;
        this.tags.addAll(tags);
    }

    /**
     * setter를 열어두지 않고 "수정"이라는 의도가 드러나는 메서드만 둔다.
     * 값을 바꾸는 곳이 이 메서드 하나라서 변경 지점을 추적하기 쉽다.
     */
    public void update(String title, String content, Set<Tag> tags) {
        this.title = title;
        this.content = content;
        // 컬렉션을 새 객체로 갈아 끼우지 않고 내용물만 바꾼다. Hibernate는 이 컬렉션을
        // 지켜보다가 빠진 태그와 새로 들어온 태그만 골라 연결 테이블에 반영한다.
        this.tags.retainAll(tags);
        this.tags.addAll(tags);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    /** 태그 이름을 가나다·알파벳 순으로 돌려준다. 순서가 고정되어야 응답이 매번 같다. */
    public List<String> getTagNames() {
        return tags.stream().map(Tag::getName).sorted().toList();
    }
}
