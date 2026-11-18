package com.example.blog.comment;

import com.example.blog.auth.Roles;
import com.example.blog.comment.dto.CommentDeleteRequest;
import com.example.blog.comment.dto.CommentRequest;
import com.example.blog.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 API. 세 주소 모두 로그인 없이 부를 수 있다.
 * 다만 관리자로 로그인한 요청이면 다르게 처리한다: 댓글에 관리자 표시가 붙고, 비밀번호 없이 지울 수 있다.
 */
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // 댓글은 글에 딸린 것이므로 주소도 글 아래에 둔다: /posts/{글 번호}/comments
    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponse> list(@PathVariable Long postId) {
        return commentService.getList(postId);
    }

    /** @param authentication 로그인한 요청이면 스프링이 채워 주고, 아니면 null이다. */
    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        String adminName = Roles.isAdmin(authentication) ? authentication.getName() : null;
        return commentService.create(postId, request, adminName);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            // 관리자는 본문 없이 요청하므로 필수가 아니다.
            @RequestBody(required = false) CommentDeleteRequest request,
            Authentication authentication) {
        String password = request == null ? null : request.password();
        commentService.delete(id, password, Roles.isAdmin(authentication));
    }
}
