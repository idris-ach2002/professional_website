package sorbonne.professional_website.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class UploadPolicy {

    private static final Set<String> SAFE_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "gif", "avif",
            "txt", "csv", "json", "doc", "docx", "ppt", "pptx"
    );
    private static final Map<String, Set<String>> MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("avif", Set.of("image/avif")),
            Map.entry("txt", Set.of("text/plain", "application/octet-stream")),
            Map.entry("csv", Set.of("text/csv", "application/vnd.ms-excel", "text/plain")),
            Map.entry("json", Set.of("application/json", "text/json", "text/plain")),
            Map.entry("doc", Set.of("application/msword", "application/octet-stream")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/octet-stream")),
            Map.entry("ppt", Set.of("application/vnd.ms-powerpoint", "application/octet-stream")),
            Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/zip", "application/octet-stream"))
    );

    private final long maxBytes;

    public UploadPolicy(@Value("${app.upload.max-bytes:10485760}") long maxBytes) {
        this.maxBytes = Math.max(1_048_576L, maxBytes);
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new StorageException("Empty upload is not allowed.");
        if (file.getSize() > maxBytes) throw new StorageException("Upload exceeds the configured size limit.");

        String extension = extension(file.getOriginalFilename());
        if (!SAFE_EXTENSIONS.contains(extension)) {
            throw new StorageException("Unsupported upload type: ." + extension);
        }

        String mime = normalizeMime(file.getContentType());
        if (mime == null) return;
        Set<String> expected = MIME_BY_EXTENSION.get(extension);
        if (expected != null && !expected.contains(mime)) {
            throw new StorageException("Upload MIME type does not match its extension.");
        }
    }

    public boolean shouldForceDownload(String filename) {
        String ext = extension(filename);
        return !Set.of("pdf", "png", "jpg", "jpeg", "webp", "gif", "avif", "txt").contains(ext);
    }

    private static String normalizeMime(String value) {
        if (value == null || value.isBlank()) return null;
        return value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        String clean = filename.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1);
        int dot = clean.lastIndexOf('.');
        if (dot < 0 || dot == clean.length() - 1) return "";
        return clean.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
