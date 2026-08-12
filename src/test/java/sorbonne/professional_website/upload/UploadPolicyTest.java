package sorbonne.professional_website.upload;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadPolicyTest {

    private final UploadPolicy policy = new UploadPolicy(10 * 1024 * 1024L);

    @Test
    void acceptsKnownImageTypeWithMatchingMagicBytes() {
        byte[] webp = new byte[]{'R','I','F','F', 0,0,0,0, 'W','E','B','P', 'V','P','8',' '};
        policy.validate(new MockMultipartFile("file", "photo.webp", "image/webp", webp));
    }

    @Test
    void acceptsPdfWithSignature() {
        policy.validate(new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "%PDF-1.7\nmock".getBytes()
        ));
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

    @Test
    void rejectsSpoofedImageEvenWhenMimeAndExtensionAgree() {
        assertThatThrownBy(() -> policy.validate(new MockMultipartFile(
                "file", "photo.png", "image/png", "<script>alert(1)</script>".getBytes()
        )))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("signature");
    }
    @Test
    void acceptsDocxOnlyWhenTheZipContainsTheExpectedOpenXmlStructure() throws IOException {
        policy.validate(new MockMultipartFile(
                "file",
                "cv.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                openXml("word/document.xml")
        ));
    }

    @Test
    void rejectsArbitraryZipRenamedAsDocx() throws IOException {
        assertThatThrownBy(() -> policy.validate(new MockMultipartFile(
                "file",
                "payload.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                openXml("not-word/payload.bin")
        )))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("signature");
    }

    private static byte[] openXml(String payloadEntry) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(payloadEntry));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
        }
        return output.toByteArray();
    }

}
