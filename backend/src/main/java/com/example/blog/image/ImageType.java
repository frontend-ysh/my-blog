package com.example.blog.image;

import java.util.Arrays;
import java.util.Optional;

/**
 * 받아들이는 이미지 형식. 파일의 첫 몇 바이트(형식마다 정해진 "서명")를 보고 무슨 형식인지 판별한다.
 *
 * <p>브라우저가 보내 주는 파일 이름(.png)이나 종류(image/png)는 보내는 쪽이 마음대로 적을 수 있어서
 * 믿을 수 없다. 이름만 .png로 바꾼 HTML·스크립트 파일이 올라와 이미지 주소로 서비스되는 것을 막는다.
 * SVG는 그림이지만 안에 스크립트를 넣을 수 있는 형식이라 받지 않는다.
 */
public enum ImageType {
    PNG("png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}),
    JPEG("jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    GIF("gif", new byte[] {'G', 'I', 'F', '8'}),
    /** WebP는 "RIFF" + 크기 4바이트 + "WEBP"로 시작한다. */
    WEBP("webp", new byte[] {'R', 'I', 'F', 'F'}) {
        @Override
        boolean matches(byte[] head) {
            return super.matches(head)
                    && head.length >= 12
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
        }
    };

    /** 형식을 판별하는 데 필요한 바이트 수. */
    static final int HEAD_LENGTH = 12;

    private final String extension;
    private final byte[] signature;

    ImageType(String extension, byte[] signature) {
        this.extension = extension;
        this.signature = signature;
    }

    public String extension() {
        return extension;
    }

    boolean matches(byte[] head) {
        return head.length >= signature.length
                && Arrays.equals(head, 0, signature.length, signature, 0, signature.length);
    }

    /** @param head 파일의 앞부분 (HEAD_LENGTH 바이트 이상이면 충분하다) */
    public static Optional<ImageType> detect(byte[] head) {
        return Arrays.stream(values()).filter(type -> type.matches(head)).findFirst();
    }
}
