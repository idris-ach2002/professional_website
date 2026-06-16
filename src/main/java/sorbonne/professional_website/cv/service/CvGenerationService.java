package sorbonne.professional_website.cv.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sorbonne.professional_website.cv.config.CvGenerationProperties;
import sorbonne.professional_website.cv.dto.CvGenerationRequest;
import sorbonne.professional_website.cv.dto.CvGenerationResponse;
import sorbonne.professional_website.cv.dto.CvSourceResponse;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.WebsiteVersionMapper;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.upload.StorageService;
import sorbonne.professional_website.upload.StoredFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CvGenerationService {

    private final WebsiteVersionRepository websiteVersionRepository;
    private final CvLatexTemplateService latexTemplateService;
    private final LatexCompileService latexCompileService;
    private final StorageService storageService;
    private final CvGenerationProperties properties;

    public CvGenerationService(
            WebsiteVersionRepository websiteVersionRepository,
            CvLatexTemplateService latexTemplateService,
            LatexCompileService latexCompileService,
            StorageService storageService,
            CvGenerationProperties properties
    ) {
        this.websiteVersionRepository = websiteVersionRepository;
        this.latexTemplateService = latexTemplateService;
        this.latexCompileService = latexCompileService;
        this.storageService = storageService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public CvSourceResponse generateSource(Long ownerId, Long versionId, CvGenerationRequest request) {
        WebsiteVersion version = findVersion(ownerId, versionId);
        String latexSource = resolveLatexSource(version, request);
        return new CvSourceResponse(latexSource, templateId(request), ownerId, versionId);
    }

    @Transactional(readOnly = true)
    public CvGenerationResponse preview(Long ownerId, Long versionId, CvGenerationRequest request) {
        WebsiteVersion version = findVersion(ownerId, versionId);
        String latexSource = resolveLatexSource(version, request);
        CompiledLatex compiledLatex = latexCompileService.compile(latexSource);
        String pdfUrl = null;
        List<String> warnings = new ArrayList<>(compiledLatex.warnings());

        if (compiledLatex.success()) {
            StoredFile storedFile = storageService.storeBytes(
                    buildGeneratedFilename(ownerId, versionId, "preview", "pdf"),
                    compiledLatex.pdfBytes()
            );
            pdfUrl = publicUrl(storedFile);
        }

        return new CvGenerationResponse(
                compiledLatex.success(),
                pdfUrl,
                latexSource,
                compiledLatex.logs(),
                warnings,
                compiledLatex.compiler(),
                ownerId,
                versionId
        );
    }

    public CvGenerationResponse save(Long ownerId, Long versionId, CvGenerationRequest request) {
        WebsiteVersion version = findVersion(ownerId, versionId);
        String latexSource = resolveLatexSource(version, request);
        CompiledLatex compiledLatex = latexCompileService.compile(latexSource);
        List<String> warnings = new ArrayList<>(compiledLatex.warnings());
        String pdfUrl = null;

        if (compiledLatex.success()) {
            StoredFile storedPdf = storageService.storeBytes(
                    buildGeneratedFilename(ownerId, versionId, "cv", "pdf"),
                    compiledLatex.pdfBytes()
            );
            pdfUrl = publicUrl(storedPdf);

            if (properties.isStoreLatexSource()) {
                storageService.storeBytes(
                        buildGeneratedFilename(ownerId, versionId, "cv-source", "tex"),
                        latexSource.getBytes(StandardCharsets.UTF_8)
                );
            }

            Profile profile = version.getProfile();
            if (profile == null) {
                profile = Profile.builder()
                        .title("CV")
                        .description("CV généré depuis le portfolio.")
                        .cvUrl(pdfUrl)
                        .build();
                version.attachProfile(profile);
            } else {
                profile.setCvUrl(pdfUrl);
            }

            websiteVersionRepository.save(version);
        }

        return new CvGenerationResponse(
                compiledLatex.success(),
                pdfUrl,
                latexSource,
                compiledLatex.logs(),
                warnings,
                compiledLatex.compiler(),
                ownerId,
                versionId
        );
    }

    @Transactional(readOnly = true)
    public WebsiteVersionResponseDTO readVersion(Long ownerId, Long versionId) {
        return WebsiteVersionMapper.toResponse(findVersion(ownerId, versionId));
    }

    private WebsiteVersion findVersion(Long ownerId, Long versionId) {
        return websiteVersionRepository.findByIdAndOwnerOwnerId(versionId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("WebsiteVersion"));
    }

    private String resolveLatexSource(WebsiteVersion version, CvGenerationRequest request) {
        if (request != null && request.latexSourceOverride() != null && !request.latexSourceOverride().isBlank()) {
            return request.latexSourceOverride();
        }
        return latexTemplateService.buildLatex(version, request);
    }

    private String templateId(CvGenerationRequest request) {
        if (request == null || request.templateId() == null || request.templateId().isBlank()) {
            return "software-engineer-latex";
        }
        return request.templateId();
    }

    private String buildGeneratedFilename(Long ownerId, Long versionId, String kind, String extension) {
        return "portfolio-owner-" + ownerId + "-version-" + versionId + "-" + kind + "." + extension;
    }

    private String publicUrl(StoredFile storedFile) {
        if (storedFile.url() != null && !storedFile.url().isBlank()) {
            return storedFile.url();
        }

        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/uploads/files/{filename}")
                .buildAndExpand(storedFile.filename())
                .toUriString();
    }
}
