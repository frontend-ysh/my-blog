package com.example.blog.image;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 저장 폴더의 파일을 /images/** 주소로 내보내도록 연결한다. */
@Configuration
public class ImageWebConfig implements WebMvcConfigurer {

    private final ImageService imageService;

    public ImageWebConfig(ImageService imageService) {
        this.imageService = imageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ImageService.URL_PREFIX + "**")
                // 폴더 주소는 "/"로 끝나야 폴더로 인식된다.
                .addResourceLocations(imageService.directory().toUri().toString())
                // 파일 이름이 매번 새로 만든 UUID라서 같은 주소의 내용은 절대 바뀌지 않는다.
                // 브라우저가 1년 동안 다시 묻지 않고 저장해 둔 것을 쓰게 한다.
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
