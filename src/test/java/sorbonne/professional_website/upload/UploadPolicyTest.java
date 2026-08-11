package sorbonne.professional_website.upload;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadPolicyTest {

    private final UploadPolicy policy = new UploadPolicy(10 * 1024 * 1024L);

    @Test
    void acceptsKnownImageType() {
        policy.validate(new MockMultipartFile("file", "photo.webp", "image/webp", new byte[]{1, 2, 3}));
    }

    @Test
    void rejectsSvgActiveContent() {
        assertThatThrownBy(() -> policy.validate(new MockMultipartFile(
                "file", "payload.svg", "image/svg+xml", "<svg><script/></svg>".getBytes()
        ))).isInstanceOf(StorageException.class);
    }

    @Test
    void rejectsExtensionMimeMismatch() {
        assertThatThrownBy(() -> policy.validate(new MockMultipartFile(
                "file", "photo.png", "text/html", "x".getBytes()
        ))).isInstanceOf(StorageException.class);
    }
}
