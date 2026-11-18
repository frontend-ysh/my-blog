package com.example.blog.tag.dto;

/**
 * 태그와, 그 태그가 붙은 글의 수.
 *
 * @param postCount JPQL의 count()는 Long을 돌려주므로 long으로 받는다.
 */
public record TagResponse(String name, long postCount) {
}
