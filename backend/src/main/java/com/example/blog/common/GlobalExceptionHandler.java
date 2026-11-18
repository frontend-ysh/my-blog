package com.example.blog.common;

import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 컨트롤러의 예외를 한곳에서 HTTP 응답으로 바꾼다.
 * 응답 형식은 표준인 Problem Details(RFC 9457)로 통일한다.
 * 부모 클래스가 잘못된 JSON, 지원하지 않는 메서드 같은 스프링 기본 예외를 처리해 주고,
 * 여기서는 우리 서비스에 필요한 것만 더한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** 애너테이션으로 표현하지 못해 코드에서 직접 검사한 입력 오류. @Valid 실패와 같은 모양으로 응답한다. */
    @ExceptionHandler(InvalidInputException.class)
    ProblemDetail handleInvalidInput(InvalidInputException e) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        body.setProperty("errors", e.getViolations());
        return body;
    }

    /** 자격이 없는 요청(예: 댓글 비밀번호가 틀림). 무엇이 문제인지 예외의 문구를 그대로 알려준다. */
    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbiddenRequest(ForbiddenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** 로그인 실패. 아이디가 없는지 비밀번호가 틀렸는지는 구분해 주지 않는다 (아이디 수집 방지). */
    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    /** 인증 실패(401): 토큰이 없거나, 만료되었거나, 위조되었다. 누구인지 알 수 없는 상태다. */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleUnauthenticated(AuthenticationException e) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                // 401에는 어떤 방식으로 인증해야 하는지 알려주는 헤더를 붙이는 것이 HTTP 규칙이다.
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(body);
    }

    /** 인가 실패(403): 누구인지는 알지만 이 일을 할 권한이 없다. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleForbidden(AccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "권한이 없습니다.");
    }

    /** @Valid 검증 실패. 어떤 필드가 왜 틀렸는지 errors 배열로 알려준다. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        // 검증기가 돌려주는 순서는 실행할 때마다 달라질 수 있으므로 정렬해서 응답을 고정한다.
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();

        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "입력값이 올바르지 않습니다.");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** 주소의 ?page=, ?size= 같은 요청 파라미터가 규칙(@Min, @Max)에 어긋난 경우. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<FieldViolation> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldViolation(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();

        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "입력값이 올바르지 않습니다.");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * 예상하지 못한 예외. 원인은 서버 로그에만 남기고,
     * 클라이언트에는 내부 정보(스택, SQL 등)가 새지 않도록 고정된 문구만 보낸다.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
    }
}
