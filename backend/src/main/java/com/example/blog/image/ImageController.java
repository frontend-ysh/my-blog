package com.example.blog.image;

import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 이미지 업로드. 관리자만 쓸 수 있다(SecurityConfig). 올린 이미지를 보는 것은 누구나 가능하다. */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    /** @param url 글 본문에 넣을 이미지 주소 */
    public record ImageResponse(String url) {
    }

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // 파일은 JSON이 아니라 multipart/form-data 형식으로 온다. 그 안의 "file" 부분을 받는다.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> upload(@RequestParam("file") MultipartFile file) {
        String url = imageService.store(file);
        return ResponseEntity.created(URI.create(url)).body(new ImageResponse(url));
    }
}
