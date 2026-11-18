package com.example.blog.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml의 blog.admin.* 설정. 서버가 처음 뜰 때 만들 관리자 계정이다. */
@ConfigurationProperties("blog.admin")
public record AdminProperties(String username, String password) {
}
