package com.example.blog.auth;

import com.example.blog.auth.dto.LoginRequest;
import com.example.blog.auth.dto.MeResponse;
import com.example.blog.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    /**
     * 아이디·비밀번호를 확인하고 토큰을 발급한다.
     * 틀리면 authenticate()가 예외를 던지고, GlobalExceptionHandler가 401로 바꾼다.
     */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        return tokenService.issue(authentication);
    }

    /**
     * 토큰의 주인이 누구인지 알려준다. 프런트엔드가 "지금 로그인 상태인가"를 확인할 때 쓴다.
     * 토큰이 없거나 만료되었으면 이 메서드에 닿기 전에 필터에서 401이 나간다.
     */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(TokenService.ROLES_CLAIM);
        return new MeResponse(jwt.getSubject(), roles == null ? List.of() : roles);
    }
}
