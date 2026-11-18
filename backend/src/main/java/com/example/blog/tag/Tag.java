package com.example.blog.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Locale;

/** tag 테이블의 한 행. 이름이 같은 태그는 하나만 존재한다. */
@Entity
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    protected Tag() {
    }

    /** @param name {@link #normalize}를 거친 이름 */
    public Tag(String name) {
        this.name = name;
    }

    /**
     * 태그 이름을 하나의 표기로 통일한다: 앞뒤 공백 제거, 소문자, 연속 공백은 한 칸.
     * "Spring", " spring ", "SPRING"이 각각 다른 태그가 되는 것을 막는다.
     */
    public static String normalize(String name) {
        return name.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
