package sorbonne.professional_website.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
@RequestMapping("/uploads")
public class FileUploadController {

    private final StorageService storageService;

    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    @ResponseBody
    public List<String> listUploadedFiles() {
        return storageService.loadAll().stream()
                .map(path -> MvcUriComponentsBuilder
                        .fromMethodName(FileUploadController.class, "serveFile", path.toString())
                        .build().toUri().toString())
                .toList();
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = storageService.loadAsResource(filename);

        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = resolveContentType(file, filename);
        String displayName = file.getFilename() == null ? filename : file.getFilename();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + displayName + "\"")
                .body(file);
    }

    private String resolveContentType(Resource file, String requestedFilename) {
        try {
            String contentType = Files.probeContentType(file.getFile().toPath());

            if (contentType != null && !contentType.isBlank()) {
                return contentType;
            }
        } catch (IOException ignored) {
            // Remote resources, including Cloudinary URLs, cannot always be resolved as local files.
        }

        String filename = requestedFilename == null || requestedFilename.isBlank()
                ? file.getFilename()
                : requestedFilename;
        filename = filename == null ? "" : filename.toLowerCase();

        if (filename.endsWith(".pdf")) return "application/pdf";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".webp")) return "image/webp";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".avif")) return "image/avif";
        if (filename.endsWith(".txt")) return "text/plain";
        if (filename.endsWith(".csv")) return "text/csv";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".doc")) return "application/msword";
        if (filename.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (filename.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (filename.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    @PostMapping("/")
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        StoredFile storedFile = storageService.store(file);
        String backendFileUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/uploads/files/{filename}")
                .buildAndExpand(storedFile.filename())
                .toUriString();

        return ResponseEntity.ok(Map.of(
                "filename", storedFile.filename(),
                "url", backendFileUrl,
                "directUrl", storedFile.url() == null ? backendFileUrl : storedFile.url()
        ));
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
}
