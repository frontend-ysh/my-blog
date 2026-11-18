package com.example.blog.image;

import com.example.blog.common.InvalidInputException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

/**
 * 올라온 이미지를 검사하고 서버의 폴더에 저장한다.
 *
 * <p>지금은 서버의 디스크에 저장한다. 서버를 여러 대로 늘리거나 서버를 새로 만들 때마다 디스크가
 * 비워지는 환경에서는 S3 같은 외부 저장소로 바꿔야 한다. 그때 이 클래스의 저장 부분만 바꾸면 된다.
 */
@Service
public class ImageService {

    /** 한 파일의 최대 크기. */
    static final DataSize MAX_SIZE = DataSize.ofMegabytes(5);

    /** 저장한 이미지를 서비스하는 주소의 앞부분. WebConfig가 이 주소를 저장 폴더에 연결한다. */
    public static final String URL_PREFIX = "/images/";

    private final Path directory;

    public ImageService(ImageProperties properties) {
        this.directory = properties.dir().toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("이미지 저장 폴더를 만들지 못했습니다: " + directory, e);
        }
    }

    /** 이미지가 저장되는 폴더(절대 경로). */
    public Path directory() {
        return directory;
    }

    /** @return 저장한 이미지의 주소 (예: /images/3f2b….png) */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidInputException("file", "파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_SIZE.toBytes()) {
            throw new InvalidInputException("file", "이미지는 5MB 이하여야 합니다.");
        }

        try (InputStream in = file.getInputStream()) {
            // 앞부분만 먼저 읽어 형식을 확인하고, 통과하면 처음부터 다시 읽어 저장한다.
            byte[] head = in.readNBytes(ImageType.HEAD_LENGTH);
            ImageType type = ImageType.detect(head)
                    .orElseThrow(() -> new InvalidInputException(
                            "file", "PNG, JPEG, GIF, WebP 이미지만 올릴 수 있습니다."));

            // 올린 사람이 붙인 파일 이름은 쓰지 않는다. "../"가 섞인 이름으로 엉뚱한 곳에 쓰게 하거나
            // 남의 파일을 덮어쓰는 것을 막고, 추측할 수 없는 이름(UUID)으로 서로 겹치지 않게 한다.
            String filename = UUID.randomUUID() + "." + type.extension();
            try (InputStream whole = file.getInputStream()) {
                Files.copy(whole, directory.resolve(filename));
            }
            return URL_PREFIX + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("이미지를 저장하지 못했습니다.", e);
        }
    }
}
