package com.example.blog.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성 요청. 이름과 비밀번호는 방문자에게만 필수이고 관리자는 보내지 않는다.
 * "누가 보냈느냐에 따라 필수 여부가 달라지는" 규칙은 애너테이션으로 쓰기 어려워서
 * CommentService에서 검사한다. 여기에는 누구에게나 같은 규칙(길이)만 둔다.
 */
public record CommentRequest(
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        String authorName,

        @Size(min = 4, max = 50, message = "비밀번호는 4자 이상 50자 이하여야 합니다.")
        String password,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 1000, message = "내용은 1000자 이하여야 합니다.")
        String content
) {
}
