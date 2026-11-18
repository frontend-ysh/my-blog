package com.example.blog.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * DB 접근 계층. 인터페이스만 선언하면 Spring Data JPA가 구현체를 만들어 준다.
 * save, findById, delete 같은 기본 메서드는 JpaRepository에 이미 들어 있다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 조건에 맞는 글을 한 페이지만큼 찾는다. 조건은 둘 다 선택이고, null이면 그 조건은 없는 것으로 친다.
     * ("값이 null이거나, 조건에 맞거나"로 써서 쿼리 하나가 네 가지 경우를 모두 처리한다.)
     *
     * @param tag     이 이름의 태그가 붙은 글만
     * @param pattern 제목이나 본문이 이 LIKE 패턴에 맞는 글만. 소문자로 된 "%검색어%" 형태여야 한다.
     *                검색어 안의 %, _ 는 호출하는 쪽에서 \ 로 이스케이프한다.
     * @param pageable 몇 번째 페이지, 몇 개씩, 어떤 순서인지. 결과인 Page에는 그 페이지의 글과 전체 개수가 들어 있다.
     */
    @Query("""
            select p from Post p
            where (:tag is null or exists (select 1 from p.tags t where t.name = :tag))
              and (:pattern is null
                   or lower(p.title) like :pattern escape '\\'
                   or lower(p.content) like :pattern escape '\\')
            """)
    Page<Post> search(@Param("tag") String tag, @Param("pattern") String pattern, Pageable pageable);
}
