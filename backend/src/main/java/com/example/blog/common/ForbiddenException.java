package com.example.blog.common;

/** 요청한 일을 할 자격이 없을 때 던지는 예외. GlobalExceptionHandler가 403으로 바꾼다. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
