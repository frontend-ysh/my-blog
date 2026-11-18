package com.example.blog.post.dto;

import com.example.blog.post.Post;
import java.time.Instant;
import java.util.List;

/** 글 목록의 한 항목. 본문(content)은 길 수 있으므로 목록에는 싣지 않는다. */
public record PostSummaryResponse(
        Long id,
        String title,
        List<String> tags,
        Instant createdAt
) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(post.getId(), post.getTitle(), post.getTagNames(), post.getCreatedAt());
    }
}
