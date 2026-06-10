package sorbonne.professional_website.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        String contentType = resolveContentType(file);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    private String resolveContentType(Resource file) {
        try {
            String contentType = Files.probeContentType(file.getFile().toPath());

            if (contentType != null && !contentType.isBlank()) {
                return contentType;
            }
        } catch (IOException ignored) {
            // Fallback below. The browser can still open unknown files as binary.
        }

        String filename = file.getFilename() == null ? "" : file.getFilename().toLowerCase();

        if (filename.endsWith(".pdf")) return "application/pdf";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".webp")) return "image/webp";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".avif")) return "image/avif";

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    @PostMapping("/")
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        storageService.store(file);

        String filename = Objects.requireNonNull(file.getOriginalFilename());
        String fileUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/uploads/files/{filename}")
                .buildAndExpand(filename)
                .toUriString();

        return ResponseEntity.ok(Map.of(
                "filename", filename,
                "url", fileUrl
        ));
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }
}
