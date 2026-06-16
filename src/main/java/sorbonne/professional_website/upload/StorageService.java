package sorbonne.professional_website.upload;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface StorageService {

    void init();

    StoredFile store(MultipartFile file);

    StoredFile storeBytes(String originalFilename, byte[] content);

    List<Path> loadAll();

    Path load(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();

}
