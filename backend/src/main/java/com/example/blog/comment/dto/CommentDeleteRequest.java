package com.example.blog.comment.dto;

/** 댓글 삭제 요청. 방문자는 댓글을 쓸 때 정한 비밀번호를 보낸다. */
public record CommentDeleteRequest(String password) {
}
