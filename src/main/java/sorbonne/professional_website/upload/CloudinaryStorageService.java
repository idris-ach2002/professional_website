package sorbonne.professional_website.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "cloudinary")
public class CloudinaryStorageService implements StorageService {

    private static final String RESOURCE_TYPE_RAW = "raw";

    private final Cloudinary cloudinary;
    private final String folder;

    public CloudinaryStorageService(CloudinaryStorageProperties properties) {
        validate(properties);

        this.folder = normalizeFolder(properties.getFolder());
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true
        ));
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file.");
        }

        String filename = FilenameUtils.createSafeUniqueFilename(file.getOriginalFilename());
        String publicId = toPublicId(filename);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", RESOURCE_TYPE_RAW,
                    "public_id", publicId,
                    "overwrite", false,
                    "use_filename", false,
                    "unique_filename", false
            ));

            Object secureUrl = uploadResult.get("secure_url");
            return new StoredFile(filename, secureUrl == null ? publicUrl(filename) : secureUrl.toString());
        } catch (Exception e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public List<Path> loadAll() {
        // Listing Cloudinary assets is not required by the current admin flow.
        // The app persists each uploaded URL in the portfolio data after upload.
        return Collections.emptyList();
    }

    @Override
    public Path load(String filename) {
        return Path.of(FilenameUtils.sanitizeExistingFilename(filename));
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            return new UrlResource(publicUrl(filename));
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        // Intentionally disabled for cloud storage.
    }

    @PostConstruct
    @Override
    public void init() {
        // Cloudinary does not require a local directory initialization.
    }

    private String publicUrl(String filename) {
        return cloudinary.url()
                .secure(true)
                .resourceType(RESOURCE_TYPE_RAW)
                .generate(toPublicId(filename));
    }

    private String toPublicId(String filename) {
        String safeFilename = FilenameUtils.sanitizeExistingFilename(filename);
        if (folder.isBlank()) {
            return safeFilename;
        }
        return folder + "/" + safeFilename;
    }

    private static void validate(CloudinaryStorageProperties properties) {
        if (isBlank(properties.getCloudName()) || isBlank(properties.getApiKey()) || isBlank(properties.getApiSecret())) {
            throw new StorageException("Cloudinary storage is enabled but CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY or CLOUDINARY_API_SECRET is missing.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeFolder(String value) {
        if (value == null || value.isBlank()) {
            return "portfolio";
        }

        return value.trim()
                .replace('\\', '/')
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }
}
