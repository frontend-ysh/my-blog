package com.example.blog.common;

import java.util.List;

/**
 * 입력값이 규칙에 어긋날 때 던지는 예외. GlobalExceptionHandler가 400과 필드별 오류로 바꾼다.
 * 애너테이션(@NotBlank 등)으로 표현하기 어려운 규칙에 쓴다.
 * 예: "방문자의 댓글에는 비밀번호가 필수지만, 관리자의 댓글에는 필요 없다".
 */
public class InvalidInputException extends RuntimeException {

    private final List<FieldViolation> violations;

    public InvalidInputException(List<FieldViolation> violations) {
        super("입력값이 올바르지 않습니다.");
        this.violations = List.copyOf(violations);
    }

    public InvalidInputException(String field, String message) {
        this(List.of(new FieldViolation(field, message)));
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }
}
