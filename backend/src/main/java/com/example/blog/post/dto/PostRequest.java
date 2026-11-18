package com.example.blog.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 글 작성·수정 요청 본문. 검증 규칙을 여기에 선언한다. */
public record PostRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        // 목록 자체의 규칙(개수)과, 목록 안 원소 하나하나의 규칙을 따로 건다.
        @Size(max = 10, message = "태그는 10개까지 붙일 수 있습니다.")
        List<
                @NotBlank(message = "빈 태그는 넣을 수 없습니다.")
                @Size(max = 30, message = "태그는 30자 이하여야 합니다.")
                @Pattern(regexp = "[^,]*", message = "태그에는 쉼표를 쓸 수 없습니다.")
                String> tags
) {

    /** 요청에 tags가 아예 없으면 "태그 없음"으로 본다. */
    public PostRequest {
        if (tags == null) {
            tags = List.of();
        }
    }
}
