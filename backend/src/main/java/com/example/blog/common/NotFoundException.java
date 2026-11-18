package com.example.blog.common;

/** 요청한 대상이 없을 때 던지는 예외. GlobalExceptionHandler가 404로 바꾼다. */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
