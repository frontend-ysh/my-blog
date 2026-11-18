package com.example.blog.tag;

import com.example.blog.tag.dto.TagResponse;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByNameIn(Collection<String> names);

    /**
     * 글이 하나라도 붙어 있는 태그를, 글이 많은 순으로 돌려준다.
     * 글에서 출발해 조인하므로 어느 글에도 쓰이지 않는 태그는 결과에 나오지 않는다.
     */
    @Query("""
            select new com.example.blog.tag.dto.TagResponse(t.name, count(p))
            from Post p join p.tags t
            group by t.name
            order by count(p) desc, t.name asc
            """)
    List<TagResponse> findAllWithPostCount();
}
