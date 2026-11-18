package com.example.blog.post;

import com.example.blog.common.PageResponse;
import com.example.blog.post.dto.PostRequest;
import com.example.blog.post.dto.PostResponse;
import com.example.blog.post.dto.PostSummaryResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP 계층. 요청을 받아 검증하고, 서비스를 호출하고, 상태 코드와 응답을 정한다.
 * 비즈니스 로직은 두지 않는다.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /** 201 Created와 함께, 새로 만들어진 글의 주소를 Location 헤더로 알려준다. */
    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest request) {
        PostResponse created = postService.create(request);
        return ResponseEntity.created(URI.create("/api/posts/" + created.id())).body(created);
    }

    /**
     * 예: GET /api/posts?page=2&size=10&tag=spring&q=검색어
     * 한 번에 가져갈 수 있는 개수에 상한을 둔다. 없으면 size=1000000 같은 요청 하나로
     * 서버와 DB에 큰 부담을 줄 수 있다.
     */
    @GetMapping
    public PageResponse<PostSummaryResponse> list(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "page는 1 이상이어야 합니다.")
            int page,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size,

            @RequestParam(required = false) String tag,

            @RequestParam(required = false)
            @Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
            String q) {
        return postService.getList(page, size, tag, q);
    }

    @GetMapping("/{id}")
    public PostResponse get(@PathVariable Long id) {
        return postService.get(id);
    }

    @PutMapping("/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.update(id, request);
    }

    /** 돌려줄 본문이 없으므로 204 No Content. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        postService.delete(id);
    }
}
