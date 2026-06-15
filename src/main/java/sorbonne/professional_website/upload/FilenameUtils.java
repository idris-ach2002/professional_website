package sorbonne.professional_website.upload;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

final class FilenameUtils {

    private FilenameUtils() {
    }

    static String createSafeUniqueFilename(String originalFilename) {
        String safeOriginal = sanitizeOriginalFilename(originalFilename);
        String extension = extractExtension(safeOriginal);
        String base = stripExtension(safeOriginal);

        if (base.isBlank()) {
            base = "file";
        }

        if (base.length() > 72) {
            base = base.substring(0, 72);
        }

        return base + "-" + UUID.randomUUID() + extension;
    }

    static String sanitizeExistingFilename(String filename) {
        String sanitized = sanitizeOriginalFilename(filename);
        if (sanitized.isBlank() || sanitized.contains("/") || sanitized.contains("\\")) {
            throw new StorageException("Invalid filename.");
        }
        return sanitized;
    }

    static String stripExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return filename;
        }
        return filename.substring(0, dotIndex);
    }

    private static String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private static String sanitizeOriginalFilename(String originalFilename) {
        String source = originalFilename == null ? "file" : originalFilename;
        String withoutPath = source.replace('\\', '/');
        withoutPath = withoutPath.substring(withoutPath.lastIndexOf('/') + 1);

        String normalized = Normalizer.normalize(withoutPath, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String sanitized = normalized
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[.-]+", "")
                .replaceAll("[.-]+$", "");

        return sanitized.isBlank() ? "file" : sanitized;
    }
}
