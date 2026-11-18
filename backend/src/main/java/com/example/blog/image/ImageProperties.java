package com.example.blog.image;

import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml의 blog.upload.* 설정.
 *
 * @param dir 올린 이미지를 저장할 폴더
 */
@Validated
@ConfigurationProperties("blog.upload")
public record ImageProperties(@NotNull Path dir) {
}
