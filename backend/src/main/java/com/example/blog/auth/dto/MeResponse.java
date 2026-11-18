package com.example.blog.auth.dto;

import java.util.List;

/** 현재 로그인한 사용자 정보. */
public record MeResponse(String username, List<String> roles) {
}
