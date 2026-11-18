package com.example.blog.post;

import com.example.blog.common.NotFoundException;

public class PostNotFoundException extends NotFoundException {

    public PostNotFoundException(Long id) {
        super("글을 찾을 수 없습니다. id=" + id);
    }
}
