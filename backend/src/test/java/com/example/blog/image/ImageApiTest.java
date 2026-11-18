package com.example.blog.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.blog.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** 이미지 업로드와 서비스 테스트. 올린 파일은 build/test-uploads 폴더에 저장된다. */
class ImageApiTest extends ApiTestSupport {

    static final String URL_PATTERN = "^/images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.%s$";

    @Autowired
    ImageService imageService;

    @BeforeEach
    void emptyUploadDirectory() throws IOException {
        for (Path file : storedFiles()) {
            Files.delete(file);
        }
    }

    @Nested
    @DisplayName("POST /api/images")
    class Upload {

        @Test
        @DisplayName("관리자가 PNG를 올리면 201과 이미지 주소를 돌려주고, 그 주소로 같은 파일을 누구나 받을 수 있다")
        void uploadsAndServesImage() throws Exception {
            byte[] png = image("png");

            String body = bodyOf(upload(file("사진.png", "image/png", png), adminToken)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.url").value(matchesPattern(URL_PATTERN.formatted("png")))));
            String url = JsonPath.parse(body).read("$.url");

            // 토큰 없이 받는다.
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andExpect(content().bytes(png))
                    // 주소가 겹칠 일이 없으므로 브라우저가 오래 저장해 두고 쓰게 한다.
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public, immutable"))
                    // 브라우저가 내용을 보고 종류를 멋대로 추측하지 못하게 한다.
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("JPEG, GIF, WebP도 받고, 확장자는 파일 내용으로 정한다")
        void acceptsOtherFormats() throws Exception {
            // 이름과 종류를 일부러 틀리게 보낸다. 내용이 진짜 형식이다.
            upload(file("a.png", "image/png", image("jpg")), adminToken)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.url").value(matchesPattern(URL_PATTERN.formatted("jpg"))));
            upload(file("b", "application/octet-stream", image("gif")), adminToken)
                    .andExpect(jsonPath("$.url").value(matchesPattern(URL_PATTERN.formatted("gif"))));
            upload(file("c.webp", "image/webp", webpHeader()), adminToken)
                    .andExpect(jsonPath("$.url").value(matchesPattern(URL_PATTERN.formatted("webp"))));
        }

        @Test
        @DisplayName("이름과 종류가 이미지여도 내용이 이미지가 아니면 거절하고 저장하지 않는다")
        void rejectsNonImagesDisguisedAsImages() throws Exception {
            byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
            byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                    .getBytes(StandardCharsets.UTF_8);

            upload(file("evil.png", "image/png", html), adminToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("file"))
                    .andExpect(jsonPath("$.errors[0].message").value("PNG, JPEG, GIF, WebP 이미지만 올릴 수 있습니다."));
            upload(file("drawing.svg", "image/svg+xml", svg), adminToken).andExpect(status().isBadRequest());
            // "RIFF"로 시작하지만 WebP가 아닌 파일(예: WAV 소리 파일)
            upload(file("sound.webp", "image/webp", "RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1)), adminToken)
                    .andExpect(status().isBadRequest());

            assertThat(storedFiles()).isEmpty();
        }

        @Test
        @DisplayName("올린 사람이 붙인 파일 이름은 쓰지 않는다 (다른 폴더에 쓰려는 이름도 소용없다)")
        void ignoresClientFilename() throws Exception {
            upload(file("../../../evil.png", "image/png", image("png")), adminToken).andExpect(status().isCreated());

            assertThat(storedFiles()).hasSize(1);
            assertThat(storedFiles().getFirst().getFileName().toString()).doesNotContain("evil");
            assertThat(imageService.directory().getParent().resolve("evil.png")).doesNotExist();
        }

        @Test
        @DisplayName("5MB까지 받고, 넘으면 거절한다")
        void limitsFileSize() throws Exception {
            byte[] exactlyFiveMegabytes = pngOfSize(5 * 1024 * 1024);
            byte[] oneByteOver = pngOfSize(5 * 1024 * 1024 + 1);

            upload(file("big.png", "image/png", exactlyFiveMegabytes), adminToken).andExpect(status().isCreated());
            upload(file("too-big.png", "image/png", oneByteOver), adminToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("이미지는 5MB 이하여야 합니다."));

            assertThat(storedFiles()).hasSize(1);
        }

        @Test
        @DisplayName("빈 파일이나 file 항목이 없는 요청은 400을 돌려준다")
        void rejectsEmptyAndMissingFile() throws Exception {
            upload(file("empty.png", "image/png", new byte[0]), adminToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].message").value("파일이 비어 있습니다."));
            mockMvc.perform(multipart("/api/images").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("로그인하지 않으면 401, 관리자가 아니면 403을 돌려주고 저장하지 않는다")
        void requiresAdmin() throws Exception {
            String userToken = tokenService.issue("someone", List.of("USER")).accessToken();

            mockMvc.perform(multipart("/api/images").file(file("a.png", "image/png", image("png"))))
                    .andExpect(status().isUnauthorized());
            upload(file("a.png", "image/png", image("png")), userToken).andExpect(status().isForbidden());

            assertThat(storedFiles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /images/{파일}")
    class Serve {

        @Test
        @DisplayName("없는 이미지는 404를 돌려준다")
        void returnsNotFound() throws Exception {
            mockMvc.perform(get("/images/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("저장 폴더 밖의 파일은 어떤 주소로도 꺼낼 수 없다")
        void cannotEscapeUploadDirectory() throws Exception {
            // 저장 폴더의 한 단계 위에, 새어 나가면 안 되는 파일을 둔다.
            Path secret = imageService.directory().getParent().resolve("secret.txt");
            Files.writeString(secret, "새어 나가면 안 되는 내용");
            try {
                for (String path : List.of(
                        "/images/../secret.txt",
                        "/images/..%2Fsecret.txt",
                        "/images/%2e%2e/secret.txt",
                        "/images/..%5Csecret.txt",
                        "/images/a/../../secret.txt")) {
                    int status = mockMvc.perform(get(java.net.URI.create(path))).andReturn().getResponse().getStatus();
                    assertThat(status).as(path).isBetween(400, 499);
                }
            } finally {
                Files.delete(secret);
            }
        }
    }

    private ResultActions upload(MockMultipartFile file, String token) throws Exception {
        return mockMvc.perform(multipart("/api/images").file(file).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private static MockMultipartFile file(String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", filename, contentType, bytes);
    }

    /** 진짜 이미지 파일(2x2 픽셀)을 만든다. */
    private static byte[] image(String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThat(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, out)).isTrue();
        return out.toByteArray();
    }

    /** 크기 검사용: PNG로 시작하고 나머지는 0으로 채운, 정확히 size 바이트짜리 파일. */
    private static byte[] pngOfSize(int size) throws IOException {
        byte[] bytes = new byte[size];
        byte[] png = image("png");
        System.arraycopy(png, 0, bytes, 0, png.length);
        return bytes;
    }

    /** WebP 파일의 머리 부분. (자바 기본 도구로는 WebP를 만들 수 없어서 형식 판별에 쓰이는 부분만 흉내 낸다.) */
    private static byte[] webpHeader() {
        return "RIFF\u0010\0\0\0WEBPVP8 \0\0\0\0".getBytes(StandardCharsets.ISO_8859_1);
    }

    private List<Path> storedFiles() throws IOException {
        try (Stream<Path> files = Files.list(imageService.directory())) {
            return files.toList();
        }
    }
}
