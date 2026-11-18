package com.example.blog.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 보안 필터에서 난 오류(토큰 없음·만료, 권한 부족)를 GlobalExceptionHandler로 넘긴다.
 *
 * <p>필터는 컨트롤러보다 앞에서 실행되기 때문에, 여기서 난 예외는 @RestControllerAdvice에
 * 저절로 닿지 않는다. 직접 넘겨 주어야 401·403 응답도 다른 오류와 같은 형식으로 나간다.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public SecurityErrorHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    /** 인증 실패: 누구인지 확인할 수 없다 → 401 */
    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        resolver.resolveException(request, response, null, exception);
    }

    /** 인가 실패: 누구인지는 알지만 권한이 없다 → 403 */
    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        resolver.resolveException(request, response, null, exception);
    }
}
