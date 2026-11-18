package com.example.blog.auth.dto;

/**
 * 로그인 성공 응답.
 *
 * @param accessToken 이후 요청의 Authorization 헤더에 "Bearer {accessToken}"으로 실어 보낸다.
 * @param tokenType   항상 "Bearer"
 * @param expiresIn   토큰이 유효한 시간(초)
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
