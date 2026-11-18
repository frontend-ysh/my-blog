package com.example.blog.comment.dto;

import com.example.blog.comment.Comment;
import java.time.Instant;

/** 댓글 응답. 비밀번호(해시)는 절대 내보내지 않는다. */
public record CommentResponse(
        Long id,
        String authorName,
        String content,
        boolean byAdmin,
        Instant createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthorName(),
                comment.getContent(),
                comment.isByAdmin(),
                comment.getCreatedAt());
    }
}
