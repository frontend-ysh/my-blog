package com.example.blog.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 목록의 한 페이지.
 * 스프링의 Page를 그대로 JSON으로 내보내면 내부 구조가 드러나고 버전에 따라 모양이 바뀔 수 있어서,
 * 필요한 값만 담은 우리 형식으로 옮겨 담는다.
 *
 * @param content       이 페이지의 항목들
 * @param page          현재 페이지 번호 (1부터 시작)
 * @param size          한 페이지의 최대 항목 수
 * @param totalElements 전체 항목 수
 * @param totalPages    전체 페이지 수
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                // 스프링의 페이지 번호는 0부터 시작한다. 주소창에 보이는 값은 1부터가 자연스럽다.
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
