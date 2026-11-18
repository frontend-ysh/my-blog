package com.example.blog.common;

/** 오류 응답의 errors 배열에 싣는 한 항목: 어느 입력이 왜 틀렸는지. */
public record FieldViolation(String field, String message) {
}
