package sorbonne.professional_website.cv.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sorbonne.professional_website.cv.config.CvGenerationProperties;
import sorbonne.professional_website.cv.dto.CvAssetDto;
import sorbonne.professional_website.cv.dto.CvExportZipResponse;
import sorbonne.professional_website.cv.dto.CvGenerationRequest;
import sorbonne.professional_website.cv.dto.CvGenerationResponse;
import sorbonne.professional_website.cv.dto.CvQualityReportResponse;
import sorbonne.professional_website.cv.dto.CvSourceResponse;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.WebsiteVersionMapper;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.upload.StorageService;
import sorbonne.professional_website.upload.StoredFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        CompiledLatex compiledLatex = latexCompileService.compile(latexSource, latexAssets(request));
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
        CompiledLatex compiledLatex = latexCompileService.compile(latexSource, latexAssets(request));
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
    public CvExportZipResponse exportZip(Long ownerId, Long versionId, CvGenerationRequest request) {
        WebsiteVersion version = findVersion(ownerId, versionId);
        String latexSource = resolveLatexSource(version, request);
        List<CvLatexAsset> assets = latexAssets(request);
        CompiledLatex compiledLatex = latexCompileService.compile(latexSource, assets);
        List<String> warnings = new ArrayList<>(compiledLatex.warnings());
        String pdfUrl = null;
        String zipUrl = null;

        if (compiledLatex.success()) {
            StoredFile storedPdf = storageService.storeBytes(
                    buildGeneratedFilename(ownerId, versionId, "cv-export", "pdf"),
                    compiledLatex.pdfBytes()
            );
            pdfUrl = publicUrl(storedPdf);
        }

        byte[] zipBytes = buildReproducibleZip(ownerId, versionId, latexSource, compiledLatex, assets);
        if (zipBytes.length > 0) {
            StoredFile storedZip = storageService.storeBytes(
                    buildGeneratedFilename(ownerId, versionId, "cv-reproducible", "zip"),
                    zipBytes
            );
            zipUrl = publicUrl(storedZip);
        } else {
            warnings.add("Export ZIP impossible : erreur interne pendant l'écriture du fichier reproductible.");
        }

        return new CvExportZipResponse(
                compiledLatex.success(),
                zipUrl,
                pdfUrl,
                compiledLatex.logs(),
                warnings,
                compiledLatex.compiler(),
                ownerId,
                versionId
        );
    }

    @Transactional(readOnly = true)
    public CvQualityReportResponse quality(Long ownerId, Long versionId, CvGenerationRequest request) {
        WebsiteVersion version = findVersion(ownerId, versionId);
        String latexSource = resolveLatexSource(version, request);
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (latexSource == null || latexSource.isBlank()) {
            blockers.add("La source LaTeX est vide.");
        }
        if (latexSource != null && latexSource.length() > 90_000) {
            warnings.add("La source LaTeX est très longue : risque de CV sur plusieurs pages.");
        }
        if (request == null || request.assets() == null || request.assets().isEmpty()) {
            warnings.add("Aucun asset image n'est envoyé : la photo et les logos école peuvent être remplacés par des placeholders.");
        }
        if (request != null && request.projectLimit() != null && request.projectLimit() > 4) {
            suggestions.add("Limiter les projets à 4 pour conserver un CV compact sur une page.");
        }
        if (request != null && request.experienceLimit() != null && request.experienceLimit() > 2) {
            suggestions.add("Limiter les expériences à 2 ou 3 pour préserver le modèle compact.");
        }
        if (latexSource != null && !latexSource.contains("\\schoollogo")) {
            suggestions.add("Le modèle ne semble pas utiliser \\schoollogo : vérifie que le template fait main est bien conservé.");
        }
        if (latexSource != null && !latexSource.contains("\\begin{tcolorbox}")) {
            warnings.add("Le template ne contient pas de tcolorbox : le rendu peut s'éloigner du modèle CV de référence.");
        }

        int estimatedPages = estimatePageCount(latexSource);
        if (estimatedPages > 1) {
            warnings.add("Le CV semble potentiellement dépasser une page. Active la compaction intelligente.");
        }

        int score = Math.max(0, 100 - blockers.size() * 35 - warnings.size() * 8 - suggestions.size() * 3);
        return new CvQualityReportResponse(score, estimatedPages, blockers, warnings, suggestions, ownerId, versionId);
    }

    @Transactional(readOnly = true)
    public WebsiteVersionResponseDTO readVersion(Long ownerId, Long versionId) {
        return WebsiteVersionMapper.toResponse(findVersion(ownerId, versionId));
    }


    private byte[] buildReproducibleZip(Long ownerId, Long versionId, String latexSource, CompiledLatex compiledLatex, List<CvLatexAsset> assets) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            writeZipEntry(zip, "main.tex", latexSource.getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "compile.log", (compiledLatex.logs() == null ? "" : compiledLatex.logs()).getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "metadata.json", ("{\n"
                    + "  \"ownerId\": " + ownerId + ",\n"
                    + "  \"versionId\": " + versionId + ",\n"
                    + "  \"compiler\": \"" + compiledLatex.compiler() + "\",\n"
                    + "  \"success\": " + compiledLatex.success() + "\n"
                    + "}\n").getBytes(StandardCharsets.UTF_8));
            if (compiledLatex.success() && compiledLatex.pdfBytes().length > 0) {
                writeZipEntry(zip, "cv.pdf", compiledLatex.pdfBytes());
            }
            for (CvLatexAsset asset : assets) {
                writeZipEntry(zip, "assets/" + asset.filename(), asset.bytes());
            }
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException ignored) {
            return new byte[0];
        }
    }

    private void writeZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(bytes == null ? new byte[0] : bytes);
        zip.closeEntry();
    }

    private int estimatePageCount(String latexSource) {
        if (latexSource == null || latexSource.isBlank()) {
            return 0;
        }
        int hardPageBreaks = latexSource.split("\\\\newpage|\\\\pagebreak", -1).length - 1;
        int itemizeCount = latexSource.split("\\\\item", -1).length - 1;
        int projectCount = latexSource.split("\\\\projectentry", -1).length - 1;
        int experienceCount = latexSource.split("\\\\experienceentry", -1).length - 1;
        int pressure = latexSource.length() / 24_000 + Math.max(0, projectCount - 4) / 2 + Math.max(0, experienceCount - 2) / 2 + Math.max(0, itemizeCount - 16) / 8;
        return Math.max(1, 1 + hardPageBreaks + Math.max(0, pressure));
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

    private List<CvLatexAsset> latexAssets(CvGenerationRequest request) {
        if (request == null || request.assets() == null || request.assets().isEmpty()) {
            return List.of();
        }

        return request.assets().stream()
                .map(this::toLatexAsset)
                .filter(asset -> asset != null)
                .toList();
    }

    private CvLatexAsset toLatexAsset(CvAssetDto asset) {
        if (asset == null || asset.filename() == null || asset.dataUrl() == null) {
            return null;
        }

        String filename = safeAssetFilename(asset.filename());
        if (filename == null) {
            return null;
        }

        byte[] bytes = decodeAssetBytes(asset.dataUrl());
        if (bytes.length == 0 || bytes.length > 4_000_000) {
            return null;
        }

        return new CvLatexAsset(filename, bytes);
    }

    private String safeAssetFilename(String rawFilename) {
        String filename = rawFilename == null ? "" : rawFilename.trim();
        if (!Pattern.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,95}", filename) || filename.contains("..")) {
            return null;
        }

        String lower = filename.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
            return null;
        }

        return filename;
    }

    private byte[] decodeAssetBytes(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            return new byte[0];
        }

        String payload = dataUrl.trim();
        int commaIndex = payload.indexOf(',');
        if (payload.startsWith("data:") && commaIndex >= 0) {
            payload = payload.substring(commaIndex + 1);
        }

        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ignored) {
            return new byte[0];
        }
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

        try {
            return ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/uploads/files/{filename}")
                    .buildAndExpand(storedFile.filename())
                    .toUriString();
        } catch (IllegalStateException ignored) {
            // Les jobs asynchrones ne disposent pas toujours d'un contexte HTTP Spring.
            // On renvoie alors une URL relative que le front peut résoudre via son API base URL.
            return "/uploads/files/" + storedFile.filename();
        }
    }
}
