package sorbonne.professional_website.applications.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sorbonne.professional_website.applications.dto.ApplicationCommandDto;
import sorbonne.professional_website.applications.dto.ApplicationDashboardResponse;
import sorbonne.professional_website.applications.dto.CoverLetterRequest;
import sorbonne.professional_website.applications.dto.CoverLetterResponse;
import sorbonne.professional_website.applications.dto.JobApplicationRequest;
import sorbonne.professional_website.applications.dto.JobApplicationResponse;
import sorbonne.professional_website.applications.dto.OfferAnalysisRequest;
import sorbonne.professional_website.applications.dto.OfferAnalysisResponse;
import sorbonne.professional_website.applications.entity.ApplicationStatus;
import sorbonne.professional_website.applications.entity.JobApplication;
import sorbonne.professional_website.applications.repository.JobApplicationRepository;
import sorbonne.professional_website.cv.dto.CvAssetDto;
import sorbonne.professional_website.cv.service.CompiledLatex;
import sorbonne.professional_website.cv.service.CvLatexAsset;
import sorbonne.professional_website.cv.service.LatexCompileService;
import sorbonne.professional_website.entity.ContactInfo;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.upload.StorageService;
import sorbonne.professional_website.upload.StoredFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional
public class JobApplicationService {

    private static final List<String> KEYWORDS = List.of(
            "java", "spring", "spring boot", "api rest", "postgresql", "sql", "hibernate", "jpa",
            "react", "javascript", "typescript", "docker", "kubernetes", "linux", "cloud", "render",
            "devops", "ci/cd", "tests", "junit", "architecture", "microservices", "git", "maven",
            "python", "data", "symfony", "tailwind", "mantine", "qualité", "agile", "scrum"
    );

    private final JobApplicationRepository jobApplicationRepository;
    private final OwnerRepository ownerRepository;
    private final WebsiteVersionRepository websiteVersionRepository;
    private final LatexCompileService latexCompileService;
    private final StorageService storageService;

    public JobApplicationService(
            JobApplicationRepository jobApplicationRepository,
            OwnerRepository ownerRepository,
            WebsiteVersionRepository websiteVersionRepository,
            LatexCompileService latexCompileService,
            StorageService storageService
    ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.ownerRepository = ownerRepository;
        this.websiteVersionRepository = websiteVersionRepository;
        this.latexCompileService = latexCompileService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> list(Long ownerId, ApplicationStatus status) {
        ensureOwner(ownerId);
        List<JobApplication> applications = status == null
                ? jobApplicationRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId)
                : jobApplicationRepository.findByOwnerIdAndStatusOrderByUpdatedAtDesc(ownerId, status);
        return applications.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationDashboardResponse dashboard(Long ownerId) {
        List<JobApplicationResponse> applications = list(ownerId, null);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            long count = applications.stream().filter(application -> status.equals(application.status())).count();
            byStatus.put(status.name(), count);
        }
        double averageScore = applications.stream()
                .map(JobApplicationResponse::relevanceScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return new ApplicationDashboardResponse(
                applications.size(),
                byStatus.getOrDefault(ApplicationStatus.DRAFT.name(), 0L) + byStatus.getOrDefault(ApplicationStatus.TO_SEND.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.SENT.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.FOLLOW_UP.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.INTERVIEW.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.ACCEPTED.name(), 0L),
                byStatus.getOrDefault(ApplicationStatus.REJECTED.name(), 0L),
                Math.round(averageScore * 10.0) / 10.0,
                byStatus
        );
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse read(Long ownerId, Long applicationId) {
        return toResponse(findApplication(ownerId, applicationId));
    }

    public JobApplicationResponse create(Long ownerId, JobApplicationRequest request) {
        ensureOwner(ownerId);
        OfferAnalysisResponse analysis = analyzeOffer(new OfferAnalysisRequest(
                request.offerText(),
                request.roleTitle(),
                request.companyName(),
                List.of(),
                List.of()
        ));

        JobApplication application = JobApplication.builder()
                .ownerId(ownerId)
                .versionId(request.versionId())
                .companyName(nonBlank(request.companyName(), "Entreprise à renseigner"))
                .roleTitle(nonBlank(request.roleTitle(), "Poste à renseigner"))
                .location(clean(request.location()))
                .offerUrl(clean(request.offerUrl()))
                .offerText(clean(request.offerText()))
                .status(request.status() == null ? ApplicationStatus.DRAFT : request.status())
                .targetProfile(nonBlank(request.targetProfile(), analysis.targetProfile()))
                .cvVariantName(clean(request.cvVariantName()))
                .cvUrl(clean(request.cvUrl()))
                .coverLetterUrl(clean(request.coverLetterUrl()))
                .mailDraft(nonBlank(request.mailDraft(), buildDefaultMailDraft(request.companyName(), request.roleTitle())))
                .coverLetterSource(clean(request.coverLetterSource()))
                .notes(clean(request.notes()))
                .relevanceScore(analysis.score())
                .matchedKeywords(joinList(analysis.matchedKeywords()))
                .missingKeywords(joinList(analysis.missingKeywords()))
                .recommendations(joinList(analysis.recommendations()))
                .appliedAt(request.appliedAt())
                .followUpAt(request.followUpAt())
                .build();

        return toResponse(jobApplicationRepository.save(application));
    }

    public JobApplicationResponse update(Long ownerId, Long applicationId, JobApplicationRequest request) {
        JobApplication application = findApplication(ownerId, applicationId);
        application.setVersionId(request.versionId() == null ? application.getVersionId() : request.versionId());
        application.setCompanyName(nonBlank(request.companyName(), application.getCompanyName()));
        application.setRoleTitle(nonBlank(request.roleTitle(), application.getRoleTitle()));
        application.setLocation(clean(request.location()));
        application.setOfferUrl(clean(request.offerUrl()));
        application.setOfferText(clean(request.offerText()));
        application.setStatus(request.status() == null ? application.getStatus() : request.status());
        application.setTargetProfile(clean(request.targetProfile()));
        application.setCvVariantName(clean(request.cvVariantName()));
        application.setCvUrl(clean(request.cvUrl()));
        application.setCoverLetterUrl(clean(request.coverLetterUrl()));
        application.setMailDraft(clean(request.mailDraft()));
        application.setCoverLetterSource(clean(request.coverLetterSource()));
        application.setNotes(clean(request.notes()));
        application.setAppliedAt(request.appliedAt());
        application.setFollowUpAt(request.followUpAt());

        OfferAnalysisResponse analysis = analyzeOffer(new OfferAnalysisRequest(
                application.getOfferText(),
                application.getRoleTitle(),
                application.getCompanyName(),
                List.of(),
                List.of()
        ));
        application.setRelevanceScore(analysis.score());
        application.setMatchedKeywords(joinList(analysis.matchedKeywords()));
        application.setMissingKeywords(joinList(analysis.missingKeywords()));
        application.setRecommendations(joinList(analysis.recommendations()));
        if (application.getTargetProfile() == null || application.getTargetProfile().isBlank()) {
            application.setTargetProfile(analysis.targetProfile());
        }

        return toResponse(jobApplicationRepository.save(application));
    }

    public void delete(Long ownerId, Long applicationId) {
        JobApplication application = findApplication(ownerId, applicationId);
        jobApplicationRepository.delete(application);
    }

    public JobApplicationResponse markStatus(Long ownerId, Long applicationId, ApplicationStatus status) {
        JobApplication application = findApplication(ownerId, applicationId);
        application.setStatus(status == null ? ApplicationStatus.DRAFT : status);
        if (ApplicationStatus.SENT.equals(status) && application.getAppliedAt() == null) {
            application.setAppliedAt(LocalDate.now());
            application.setFollowUpAt(LocalDate.now().plusDays(7));
        }
        return toResponse(jobApplicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public OfferAnalysisResponse analyzeOffer(OfferAnalysisRequest request) {
        String offerText = clean(request.offerText());
        String haystack = normalize(offerText + " " + clean(request.roleTitle()) + " " + clean(request.companyName()));

        List<String> matched = KEYWORDS.stream()
                .filter(keyword -> haystack.contains(normalize(keyword)))
                .distinct()
                .toList();
        List<String> missing = KEYWORDS.stream()
                .filter(keyword -> importantKeyword(keyword) && !matched.contains(keyword))
                .limit(8)
                .toList();

        Set<String> portfolioSkills = new LinkedHashSet<>(request.portfolioSkills() == null ? List.of() : request.portfolioSkills());
        List<String> recommendedSkills = matched.stream()
                .filter(keyword -> portfolioSkills.isEmpty() || portfolioSkills.stream().noneMatch(skill -> normalize(skill).contains(normalize(keyword))))
                .limit(10)
                .toList();

        List<String> recommendedProjects = recommendProjects(request.projectTitles(), matched);
        String targetProfile = inferTargetProfile(matched, request.roleTitle());
        List<String> recommendations = buildRecommendations(matched, missing, targetProfile);
        List<ApplicationCommandDto> commands = buildCommands(matched, targetProfile);
        int score = scoreOffer(matched, missing, offerText);

        return new OfferAnalysisResponse(
                score,
                targetProfile,
                matched,
                missing,
                recommendedSkills,
                recommendedProjects,
                recommendations,
                commands,
                buildOfferSummary(request, matched, targetProfile)
        );
    }

    public CoverLetterResponse previewCoverLetter(Long ownerId, Long applicationId, CoverLetterRequest request) {
        JobApplication application = findApplication(ownerId, applicationId);
        String latexSource = resolveCoverLetterSource(ownerId, application, request);
        CompiledLatex compiled = latexCompileService.compile(latexSource, latexAssets(request));
        String pdfUrl = null;
        if (compiled.success()) {
            StoredFile pdf = storageService.storeBytes(
                    buildApplicationFilename(ownerId, applicationId, "cover-letter-preview", "pdf"),
                    compiled.pdfBytes()
            );
            pdfUrl = publicUrl(pdf);
        }
        return new CoverLetterResponse(
                compiled.success(),
                pdfUrl,
                null,
                latexSource,
                compiled.logs(),
                compiled.warnings(),
                compiled.compiler(),
                applicationId,
                ownerId
        );
    }

    public CoverLetterResponse saveCoverLetter(Long ownerId, Long applicationId, CoverLetterRequest request) {
        JobApplication application = findApplication(ownerId, applicationId);
        String latexSource = resolveCoverLetterSource(ownerId, application, request);
        CompiledLatex compiled = latexCompileService.compile(latexSource, latexAssets(request));
        String pdfUrl = null;
        if (compiled.success()) {
            StoredFile pdf = storageService.storeBytes(
                    buildApplicationFilename(ownerId, applicationId, "cover-letter", "pdf"),
                    compiled.pdfBytes()
            );
            pdfUrl = publicUrl(pdf);
            application.setCoverLetterUrl(pdfUrl);
            application.setCoverLetterSource(latexSource);
            jobApplicationRepository.save(application);
        }
        return new CoverLetterResponse(
                compiled.success(),
                pdfUrl,
                null,
                latexSource,
                compiled.logs(),
                compiled.warnings(),
                compiled.compiler(),
                applicationId,
                ownerId
        );
    }

    public CoverLetterResponse exportApplicationZip(Long ownerId, Long applicationId, CoverLetterRequest request) {
        JobApplication application = findApplication(ownerId, applicationId);
        String latexSource = resolveCoverLetterSource(ownerId, application, request);
        CompiledLatex compiled = latexCompileService.compile(latexSource, latexAssets(request));
        byte[] zipBytes = buildApplicationZip(ownerId, application, latexSource, compiled);
        StoredFile zip = storageService.storeBytes(
                buildApplicationFilename(ownerId, applicationId, "application-package", "zip"),
                zipBytes
        );
        String zipUrl = publicUrl(zip);
        application.setApplicationZipUrl(zipUrl);
        if (compiled.success() && application.getCoverLetterUrl() == null) {
            StoredFile letter = storageService.storeBytes(
                    buildApplicationFilename(ownerId, applicationId, "cover-letter", "pdf"),
                    compiled.pdfBytes()
            );
            application.setCoverLetterUrl(publicUrl(letter));
        }
        application.setCoverLetterSource(latexSource);
        jobApplicationRepository.save(application);
        return new CoverLetterResponse(
                compiled.success(),
                application.getCoverLetterUrl(),
                zipUrl,
                latexSource,
                compiled.logs(),
                compiled.warnings(),
                compiled.compiler(),
                applicationId,
                ownerId
        );
    }

    private byte[] buildApplicationZip(Long ownerId, JobApplication application, String latexSource, CompiledLatex compiled) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            writeZipEntry(zip, "lettre-motivation.tex", latexSource.getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "mail-candidature.txt", clean(application.getMailDraft()).getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "offre.txt", clean(application.getOfferText()).getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "compile.log", clean(compiled.logs()).getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "metadata.json", buildMetadataJson(ownerId, application, compiled).getBytes(StandardCharsets.UTF_8));
            if (compiled.success()) {
                writeZipEntry(zip, "lettre-motivation.pdf", compiled.pdfBytes());
            }
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException exception) {
            return ("Export ZIP impossible : " + exception.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }

    private String buildMetadataJson(Long ownerId, JobApplication application, CompiledLatex compiled) {
        return "{\n"
                + "  \"ownerId\": " + ownerId + ",\n"
                + "  \"applicationId\": " + application.getId() + ",\n"
                + "  \"companyName\": \"" + jsonEscape(application.getCompanyName()) + "\",\n"
                + "  \"roleTitle\": \"" + jsonEscape(application.getRoleTitle()) + "\",\n"
                + "  \"status\": \"" + application.getStatus() + "\",\n"
                + "  \"score\": " + (application.getRelevanceScore() == null ? 0 : application.getRelevanceScore()) + ",\n"
                + "  \"compiler\": \"" + jsonEscape(compiled.compiler()) + "\",\n"
                + "  \"success\": " + compiled.success() + "\n"
                + "}\n";
    }

    private String resolveCoverLetterSource(Long ownerId, JobApplication application, CoverLetterRequest request) {
        if (request != null && request.latexSourceOverride() != null && !request.latexSourceOverride().isBlank()) {
            return request.latexSourceOverride();
        }
        if (application.getCoverLetterSource() != null && !application.getCoverLetterSource().isBlank()) {
            return application.getCoverLetterSource();
        }
        WebsiteVersion version = resolveVersion(ownerId, request == null ? application.getVersionId() : request.versionId(), application.getVersionId());
        return buildCoverLetterLatex(version, application, request == null ? null : request.motivationTextOverride());
    }

    private WebsiteVersion resolveVersion(Long ownerId, Long requestedVersionId, Long applicationVersionId) {
        Long versionId = requestedVersionId != null ? requestedVersionId : applicationVersionId;
        if (versionId != null) {
            return websiteVersionRepository.findByIdAndOwnerOwnerId(versionId, ownerId)
                    .orElseThrow(() -> new ResourceNotFoundException("WebsiteVersion"));
        }
        return websiteVersionRepository.findByOwnerOwnerIdAndActiveTrue(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("WebsiteVersion active"));
    }

    private String buildCoverLetterLatex(WebsiteVersion version, JobApplication application, String motivationOverride) {
        Owner owner = version.getOwner();
        Profile profile = version.getProfile();
        String fullName = clean(owner.getFirstName()) + " " + clean(owner.getName());
        String email = contact(owner, "EMAIL");
        String phone = contact(owner, "PHONE_NUMBER", "PHONE");
        String linkedin = contact(owner, "LINKEDIN");
        String portfolio = profile == null ? "" : clean(profile.getPortfolioUrl());
        String headline = nonBlank(profile == null ? null : profile.getHeadline(), profile == null ? "" : profile.getDescription());
        String motivation = nonBlank(motivationOverride, buildMotivationParagraph(application, version));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE));

        return """
                \\documentclass[11pt,a4paper]{article}
                \\usepackage[margin=1.65cm]{geometry}
                \\usepackage[T1]{fontenc}
                \\usepackage[utf8]{inputenc}
                \\usepackage[french]{babel}
                \\usepackage{xcolor}
                \\usepackage{hyperref}
                \\usepackage{microtype}
                \\usepackage{enumitem}
                \\definecolor{primary}{HTML}{6E877E}
                \\definecolor{ink}{HTML}{172026}
                \\definecolor{muted}{HTML}{607076}
                \\hypersetup{colorlinks=true,urlcolor=primary,linkcolor=primary}
                \\setlength{\\parindent}{0pt}
                \\setlength{\\parskip}{0.72em}
                \\renewcommand{\\familydefault}{\\sfdefault}
                \\begin{document}
                {\\Huge\\bfseries\\color{ink} %s}\\par
                {\\large\\bfseries\\color{primary} Candidature — %s}\\par
                {\\color{muted}%s \\quad %s \\quad %s \\quad %s}\\par
                \\vspace{0.8em}
                \\hrule
                \\vspace{1.2em}
                \\begin{flushright}
                %s, le %s
                \\end{flushright}
                \\textbf{Objet : Candidature pour %s — %s}\\par
                Madame, Monsieur,\\par
                %s\\par
                Mon profil s'appuie sur %s. Cette base technique me permet de contribuer à des applications fiables, maintenables et utiles aux utilisateurs.\\par
                Je serais disponible pour échanger sur cette opportunité et préciser la manière dont mes projets peuvent répondre à vos besoins.\\par
                Je vous prie d'agréer, Madame, Monsieur, l'expression de mes salutations distinguées.\\par
                \\vspace{1.5em}
                \\textbf{%s}
                \\end{document}
                """.formatted(
                escapeLatex(fullName),
                escapeLatex(clean(application.getRoleTitle())),
                escapeLatex(email),
                escapeLatex(phone),
                latexHref(linkedin, "LinkedIn"),
                latexHref(portfolio, "Portfolio"),
                escapeLatex(nonBlank(profile == null ? null : profile.getLocation(), owner.getAddress())),
                escapeLatex(today),
                escapeLatex(clean(application.getRoleTitle())),
                escapeLatex(clean(application.getCompanyName())),
                escapeLatex(motivation),
                escapeLatex(shorten(headline, 260)),
                escapeLatex(fullName)
        );
    }

    private String buildMotivationParagraph(JobApplication application, WebsiteVersion version) {
        String company = nonBlank(application.getCompanyName(), "votre entreprise");
        String role = nonBlank(application.getRoleTitle(), "ce poste");
        String projectProof = version.getProjects() == null ? "" : version.getProjects().stream()
                .filter(project -> Boolean.TRUE.equals(project.getPublished()))
                .sorted(Comparator.comparing(project -> project.getDisplayOrder() == null ? 999 : project.getDisplayOrder()))
                .limit(2)
                .map(Project::getTitle)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + " et " + b)
                .orElse("");
        String proofSentence = projectProof.isBlank() ? "" : " Mes projets " + projectProof + " illustrent cette approche par la pratique.";
        return "Je souhaite rejoindre " + company + " sur " + role + " afin de mettre à profit mon expérience en développement logiciel, architecture web et qualité applicative." + proofSentence;
    }

    private List<String> recommendProjects(List<String> projectTitles, List<String> matched) {
        if (projectTitles == null || projectTitles.isEmpty()) {
            return List.of();
        }
        List<String> normalizedMatches = matched.stream().map(this::normalize).toList();
        return projectTitles.stream()
                .filter(Objects::nonNull)
                .filter(title -> normalizedMatches.stream().anyMatch(keyword -> normalize(title).contains(keyword))
                        || normalize(title).contains("portfolio")
                        || normalize(title).contains("ais"))
                .limit(5)
                .toList();
    }

    private List<String> buildRecommendations(List<String> matched, List<String> missing, String targetProfile) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Créer une variante de CV ciblée : " + targetProfile + ".");
        if (matched.stream().anyMatch(keyword -> keyword.contains("spring") || keyword.contains("java"))) {
            recommendations.add("Remonter les projets Java / Spring Boot et citer explicitement API REST, JPA et PostgreSQL.");
        }
        if (matched.stream().anyMatch(keyword -> keyword.contains("docker") || keyword.contains("devops") || keyword.contains("kubernetes"))) {
            recommendations.add("Mettre en avant le déploiement Docker/Render/Cloudflare et l'architecture backend.");
        }
        if (!missing.isEmpty()) {
            recommendations.add("Ajouter ou reformuler les mots-clés manquants pertinents : " + String.join(", ", missing) + ".");
        }
        recommendations.add("Limiter le CV à 2 expériences et 4 projets pour préserver le modèle une page.");
        return recommendations;
    }

    private List<ApplicationCommandDto> buildCommands(List<String> matched, String targetProfile) {
        List<ApplicationCommandDto> commands = new ArrayList<>();
        commands.add(new ApplicationCommandDto("SET_TITLE", "profile.title", targetProfile));
        commands.add(new ApplicationCommandDto("LIMIT_EXPERIENCES", "settings.experienceLimit", "2"));
        commands.add(new ApplicationCommandDto("LIMIT_PROJECTS", "settings.projectLimit", "4"));
        commands.add(new ApplicationCommandDto("SET_DENSITY", "settings.density", "compact"));
        if (!matched.isEmpty()) {
            commands.add(new ApplicationCommandDto("PRIORITIZE_SKILLS", "skills", String.join(", ", matched.stream().limit(8).toList())));
        }
        return commands;
    }

    private String inferTargetProfile(List<String> matched, String roleTitle) {
        String role = normalize(roleTitle);
        if (role.contains("devops") || matched.contains("docker") || matched.contains("kubernetes")) {
            return "Alternance Développeur Full Stack / DevOps";
        }
        if (role.contains("data") || matched.contains("python") || matched.contains("data")) {
            return "Alternance Développeur Data / Backend";
        }
        if (matched.contains("react") || matched.contains("javascript") || matched.contains("typescript")) {
            return "Alternance Développeur Full Stack";
        }
        return "Alternance Développeur Java Spring Boot";
    }

    private int scoreOffer(List<String> matched, List<String> missing, String offerText) {
        int base = Math.min(75, matched.size() * 7);
        int completeness = offerText == null || offerText.length() < 300 ? 5 : 15;
        int penalty = Math.min(25, missing.size() * 3);
        return Math.max(0, Math.min(100, base + completeness - penalty));
    }

    private boolean importantKeyword(String keyword) {
        return List.of("java", "spring boot", "api rest", "postgresql", "react", "docker", "tests", "architecture").contains(keyword);
    }

    private String buildOfferSummary(OfferAnalysisRequest request, List<String> matched, String targetProfile) {
        String company = nonBlank(request.companyName(), "Entreprise non renseignée");
        String role = nonBlank(request.roleTitle(), "Poste non renseigné");
        return company + " — " + role + " : profil conseillé " + targetProfile + ", mots-clés détectés " + String.join(", ", matched.stream().limit(8).toList()) + ".";
    }

    private String buildDefaultMailDraft(String companyName, String roleTitle) {
        return "Bonjour,\n\nJe me permets de vous transmettre ma candidature pour "
                + nonBlank(roleTitle, "votre offre")
                + " au sein de "
                + nonBlank(companyName, "votre entreprise")
                + ". Vous trouverez mon CV et ma lettre de motivation en pièces jointes.\n\nCordialement,\nIdris ACHABOU";
    }

    private JobApplicationResponse toResponse(JobApplication application) {
        return new JobApplicationResponse(
                application.getId(),
                application.getOwnerId(),
                application.getVersionId(),
                application.getCompanyName(),
                application.getRoleTitle(),
                application.getLocation(),
                application.getOfferUrl(),
                application.getOfferText(),
                application.getStatus(),
                application.getTargetProfile(),
                application.getCvVariantName(),
                application.getCvUrl(),
                application.getCoverLetterUrl(),
                application.getApplicationZipUrl(),
                application.getMailDraft(),
                application.getCoverLetterSource(),
                application.getNotes(),
                application.getRelevanceScore(),
                splitList(application.getMatchedKeywords()),
                splitList(application.getMissingKeywords()),
                splitList(application.getRecommendations()),
                application.getAppliedAt(),
                application.getFollowUpAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private JobApplication findApplication(Long ownerId, Long applicationId) {
        return jobApplicationRepository.findByIdAndOwnerId(applicationId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication"));
    }

    private void ensureOwner(Long ownerId) {
        ownerRepository.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("Owner"));
    }

    private String contact(Owner owner, String... types) {
        if (owner == null || owner.getContacts() == null) return "";
        Set<String> wanted = new LinkedHashSet<>(List.of(types));
        return owner.getContacts().stream()
                .filter(contact -> contact != null && contact.getType() != null && wanted.contains(contact.getType().name()))
                .map(ContactInfo::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }

    private List<CvLatexAsset> latexAssets(CoverLetterRequest request) {
        if (request == null || request.assets() == null || request.assets().isEmpty()) {
            return List.of();
        }
        return request.assets().stream().map(this::toLatexAsset).filter(Objects::nonNull).toList();
    }

    private CvLatexAsset toLatexAsset(CvAssetDto asset) {
        if (asset == null || asset.filename() == null || asset.dataUrl() == null) return null;
        String filename = asset.filename().trim();
        if (!Pattern.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,95}", filename) || filename.contains("..")) return null;
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) return null;
        byte[] bytes = decodeAssetBytes(asset.dataUrl());
        if (bytes.length == 0 || bytes.length > 4_000_000) return null;
        return new CvLatexAsset(filename, bytes);
    }

    private byte[] decodeAssetBytes(String dataUrl) {
        String payload = dataUrl == null ? "" : dataUrl.trim();
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

    private void writeZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes == null ? new byte[0] : bytes);
        zip.closeEntry();
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
            return "/uploads/files/" + storedFile.filename();
        }
    }

    private String buildApplicationFilename(Long ownerId, Long applicationId, String kind, String extension) {
        return "portfolio-owner-" + ownerId + "-application-" + applicationId + "-" + kind + "." + extension;
    }

    private String latexHref(String url, String label) {
        String cleanUrl = clean(url);
        String cleanLabel = escapeLatex(label);
        if (cleanUrl.isBlank()) return cleanLabel;
        return "\\href{" + escapeLatex(cleanUrl) + "}{" + cleanLabel + "}";
    }

    private String escapeLatex(String value) {
        return clean(value)
                .replace("\\", "\\textbackslash{}")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}");
    }

    private String normalize(String value) {
        String lower = clean(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String nonBlank(String value, String fallback) {
        String cleanValue = clean(value);
        return cleanValue.isBlank() ? clean(fallback) : cleanValue;
    }

    private String shorten(String value, int maxLength) {
        String text = clean(value);
        if (text.length() <= maxLength) return text;
        return text.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return String.join("\n", values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).toList());
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) return List.of();
        return List.of(value.split("\\R")).stream().map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private String jsonEscape(String value) {
        return clean(value).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
