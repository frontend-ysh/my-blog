package com.example.blog.post.dto;

import com.example.blog.post.Post;
import java.time.Instant;
import java.util.List;

/** 글 상세 응답. 엔티티를 그대로 내보내지 않고 필요한 값만 옮겨 담는다. */
public record PostResponse(
        Long id,
        String title,
        String content,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getTagNames(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
