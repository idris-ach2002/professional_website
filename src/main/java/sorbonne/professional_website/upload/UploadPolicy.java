package sorbonne.professional_website.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        if (mime != null) {
            Set<String> expected = MIME_BY_EXTENSION.get(extension);
            if (expected != null && !expected.contains(mime)) {
                throw new StorageException("Upload MIME type does not match its extension.");
            }
        }

        validateSignature(file, extension);
    }

    public boolean shouldForceDownload(String filename) {
        String ext = extension(filename);
        return !Set.of("pdf", "png", "jpg", "jpeg", "webp", "gif", "avif", "txt").contains(ext);
    }

    private static void validateSignature(MultipartFile file, String extension) {
        if (Set.of("txt", "csv", "json").contains(extension)) return;

        byte[] prefix = readPrefix(file, 32);
        boolean valid = switch (extension) {
            case "pdf" -> startsWith(prefix, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case "png" -> startsWith(prefix, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "jpg", "jpeg" -> startsWith(prefix, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "gif" -> startsWith(prefix, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(prefix, "GIF89a".getBytes(StandardCharsets.US_ASCII));
            case "webp" -> prefix.length >= 12
                    && ascii(prefix, 0, 4).equals("RIFF")
                    && ascii(prefix, 8, 4).equals("WEBP");
            case "avif" -> containsAscii(prefix, "ftypavif") || containsAscii(prefix, "ftypavis");
            case "doc", "ppt" -> startsWith(prefix, new byte[]{
                    (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                    (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
            });
            case "docx", "pptx" -> startsWith(prefix, new byte[]{0x50, 0x4B, 0x03, 0x04})
                    && hasExpectedOpenXmlStructure(file, extension);
            default -> false;
        };

        if (!valid) {
            throw new StorageException("Upload content signature does not match its extension.");
        }
    }


    private static boolean hasExpectedOpenXmlStructure(MultipartFile file, String extension) {
        String requiredRoot = "docx".equals(extension) ? "word/" : "ppt/";
        boolean contentTypes = false;
        boolean documentRoot = false;
        int inspectedEntries = 0;

        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null && inspectedEntries < 256) {
                inspectedEntries += 1;
                String name = entry.getName();
                if ("[Content_Types].xml".equals(name)) contentTypes = true;
                if (name != null && name.startsWith(requiredRoot)) documentRoot = true;
                if (contentTypes && documentRoot) return true;
            }
        } catch (IOException exception) {
            throw new StorageException("Upload Office document cannot be inspected.", exception);
        }
        return false;
    }

    private static byte[] readPrefix(MultipartFile file, int maxBytes) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(maxBytes);
        } catch (IOException exception) {
            throw new StorageException("Upload content cannot be inspected.", exception);
        }
    }

    private static boolean startsWith(byte[] input, byte[] prefix) {
        return input.length >= prefix.length && Arrays.equals(Arrays.copyOf(input, prefix.length), prefix);
    }

    private static String ascii(byte[] input, int offset, int length) {
        if (input.length < offset + length) return "";
        return new String(input, offset, length, StandardCharsets.US_ASCII);
    }

    private static boolean containsAscii(byte[] input, String needle) {
        return new String(input, StandardCharsets.ISO_8859_1).contains(needle);
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
