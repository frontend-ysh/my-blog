package com.example.blog.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 한 글의 댓글을 쓴 순서대로. (이름 풀이: post의 id가 같은 것을, createdAt 오름차순 → id 오름차순) */
    List<Comment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId);
}
