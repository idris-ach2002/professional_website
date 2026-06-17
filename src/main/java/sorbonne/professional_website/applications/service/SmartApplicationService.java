package sorbonne.professional_website.applications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sorbonne.professional_website.applications.dto.ApplicationCommandDto;
import sorbonne.professional_website.applications.dto.CandidateEvidenceDto;
import sorbonne.professional_website.applications.dto.CvVariantProposalDto;
import sorbonne.professional_website.applications.dto.LetterTemplateResponse;
import sorbonne.professional_website.applications.dto.LetterVariantProposalDto;
import sorbonne.professional_website.applications.dto.MailVariantProposalDto;
import sorbonne.professional_website.applications.dto.MatchingScoreDto;
import sorbonne.professional_website.applications.dto.SmartApplicationPackResponse;
import sorbonne.professional_website.applications.dto.SmartOfferAnalysisResponse;
import sorbonne.professional_website.applications.dto.StructuredOfferDto;
import sorbonne.professional_website.applications.entity.JobApplication;
import sorbonne.professional_website.applications.repository.JobApplicationRepository;
import sorbonne.professional_website.cv.service.CompiledLatex;
import sorbonne.professional_website.cv.service.LatexCompileService;
import sorbonne.professional_website.entity.ContactInfo;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional
public class SmartApplicationService {

    private static final List<SkillRule> SKILL_RULES = List.of(
            new SkillRule("Java", "backend", 12, List.of("java", "jdk", "jvm")),
            new SkillRule("Spring Boot", "backend", 14, List.of("spring boot", "spring", "springboot")),
            new SkillRule("API REST", "backend", 12, List.of("api rest", "rest", "endpoint", "web service", "web services")),
            new SkillRule("PostgreSQL", "data", 11, List.of("postgresql", "postgres", "sql", "base de données", "bdd")),
            new SkillRule("JPA / Hibernate", "backend", 10, List.of("jpa", "hibernate", "orm")),
            new SkillRule("React", "frontend", 10, List.of("react", "front-end", "frontend", "ui")),
            new SkillRule("JavaScript", "frontend", 8, List.of("javascript", "js", "ecmascript")),
            new SkillRule("TypeScript", "frontend", 9, List.of("typescript", "ts")),
            new SkillRule("Docker", "devops", 10, List.of("docker", "container", "conteneur")),
            new SkillRule("Kubernetes", "devops", 9, List.of("kubernetes", "k8s")),
            new SkillRule("CI/CD", "devops", 9, List.of("ci/cd", "pipeline", "gitlab ci", "github actions", "déploiement continu")),
            new SkillRule("Tests", "quality", 9, List.of("tests", "junit", "test", "qualité", "recette")),
            new SkillRule("Architecture logicielle", "architecture", 12, List.of("architecture", "conception", "design", "maintenabilité", "scalable")),
            new SkillRule("Linux", "system", 8, List.of("linux", "ubuntu", "systemd", "shell", "bash")),
            new SkillRule("Python", "data", 8, List.of("python", "script", "scripting")),
            new SkillRule("Data pipeline", "data", 10, List.of("data", "pipeline", "ingestion", "csv", "traitement de données")),
            new SkillRule("Agile / Scrum", "team", 6, List.of("agile", "scrum", "sprint", "équipe")),
            new SkillRule("Sécurité", "security", 8, List.of("sécurité", "security", "authentification", "autorisation", "jwt")),
            new SkillRule("Cloud", "devops", 7, List.of("cloud", "render", "cloudflare", "railway", "aws", "azure"))
    );

    private static final List<String> SOFT_SKILL_RULES = List.of(
            "rigueur", "autonomie", "curiosité", "communication", "travail en équipe", "analyse", "adaptabilité", "documentation"
    );

    private static final List<LetterTemplateResponse> BUILT_IN_TEMPLATES = List.of(
            new LetterTemplateResponse("technical-direct", "Technique directe", "BACKEND", "formel", 95, List.of("Java", "Spring Boot", "API REST", "PostgreSQL"), "Prouver rapidement l’adéquation technique avec l’offre.", "Accroche courte → preuves techniques → adéquation missions → disponibilité", true),
            new LetterTemplateResponse("industrial-critical", "Systèmes critiques / industriel", "INDUSTRIAL", "très formel", 88, List.of("architecture", "qualité", "tests", "fiabilité"), "Insister sur la rigueur, la qualité logicielle et les environnements exigeants.", "Entreprise → rigueur → preuves → alternance → conclusion", true),
            new LetterTemplateResponse("fullstack-product", "Full stack orienté produit", "FULLSTACK", "professionnel", 82, List.of("React", "Spring Boot", "UX", "API REST"), "Relier backend, front et expérience utilisateur.", "Produit → stack → projet portfolio → impact utilisateur", true),
            new LetterTemplateResponse("data-backend", "Data / backend", "DATA", "technique", 86, List.of("PostgreSQL", "Python", "pipeline", "Java"), "Valoriser le traitement de données et la structuration backend.", "Données → pipeline → robustesse → contribution", true),
            new LetterTemplateResponse("devops-platform", "DevOps / plateforme", "DEVOPS", "technique", 84, List.of("Docker", "Kubernetes", "CI/CD", "Cloud"), "Mettre en avant le déploiement, l’industrialisation et la reproductibilité.", "Infra → conteneurisation → automatisation → qualité", true),
            new LetterTemplateResponse("short-alternance", "Alternance courte et efficace", "SHORT", "sobre", 62, List.of("alternance", "disponibilité", "formation"), "Lettre compacte pour candidatures rapides.", "Objet → profil → preuve principale → échange", true),
            new LetterTemplateResponse("ats-rh", "ATS / RH mots-clés", "ATS", "clair", 72, List.of("mots-clés", "missions", "soft skills"), "Reprendre les termes exacts de l’offre sans surcharger.", "Mots-clés offre → formation → preuves → soft skills", true),
            new LetterTemplateResponse("research-lab", "Recherche / laboratoire", "RESEARCH", "académique", 80, List.of("recherche", "data", "Java", "documentation"), "Mettre en avant méthode, expérimentation et autonomie.", "Contexte recherche → stage → méthode → contribution", true),
            new LetterTemplateResponse("esn-client", "ESN / client", "ESN", "professionnel", 76, List.of("agile", "client", "équipe", "qualité"), "Prouver adaptabilité, compréhension besoin et production fiable.", "Client → équipe → adaptabilité → stack", true),
            new LetterTemplateResponse("startup-impact", "Startup / impact produit", "STARTUP", "direct", 74, List.of("React", "produit", "rapidité", "autonomie"), "Mettre en avant autonomie et capacité à livrer vite.", "Produit → autonomie → projet concret → impact", true),
            new LetterTemplateResponse("spontaneous-java", "Spontanée Java backend", "SPONTANEOUS", "formel", 78, List.of("Java", "Spring Boot", "architecture"), "Candidature spontanée ciblée backend.", "Intérêt entreprise → profil → stack → proposition d’échange", true),
            new LetterTemplateResponse("follow-up", "Relance après candidature", "FOLLOW_UP", "courtois", 45, List.of("relance", "candidature", "disponibilité"), "Relancer proprement sans paraître insistant.", "Rappel → intérêt → disponibilité → formule courte", true)
    );

    private final JobApplicationRepository jobApplicationRepository;
    private final WebsiteVersionRepository websiteVersionRepository;
    private final LatexCompileService latexCompileService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public SmartApplicationService(
            JobApplicationRepository jobApplicationRepository,
            WebsiteVersionRepository websiteVersionRepository,
            LatexCompileService latexCompileService,
            StorageService storageService,
            ObjectMapper objectMapper
    ) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.websiteVersionRepository = websiteVersionRepository;
        this.latexCompileService = latexCompileService;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<LetterTemplateResponse> listLetterTemplates() {
        return BUILT_IN_TEMPLATES;
    }

    @Transactional(readOnly = true)
    public SmartOfferAnalysisResponse analyzeSmart(Long ownerId, Long applicationId, Long versionId) {
        JobApplication application = findApplication(ownerId, applicationId);
        WebsiteVersion version = resolveVersion(ownerId, versionId, application.getVersionId());
        StructuredOfferDto offer = structureOffer(application);
        List<CandidateEvidenceDto> evidence = buildEvidence(version, offer);
        MatchingScoreDto scores = score(offer, evidence);
        List<String> matched = mergeMatchedKeywords(offer, evidence);
        List<String> missing = missingCriticalKeywords(offer, evidence);
        List<String> recommendations = buildSmartRecommendations(offer, scores, evidence, missing);
        List<String> risks = buildRiskWarnings(application, offer, scores, evidence);
        List<CvVariantProposalDto> cvVariants = buildCvVariants(application, offer, evidence, matched, missing);
        List<LetterVariantProposalDto> letterVariants = buildLetterVariants(version, application, offer, evidence, matched);
        MailVariantProposalDto mail = buildMailVariant(application, offer, evidence);
        List<ApplicationCommandDto> recommendedCommands = cvVariants.isEmpty() ? List.of() : cvVariants.get(0).commands();
        String explanation = buildExplanation(offer, scores, evidence, cvVariants, letterVariants);

        return new SmartOfferAnalysisResponse(
                "smart-" + UUID.randomUUID(),
                ownerId,
                applicationId,
                version.getId(),
                offer,
                scores,
                matched,
                missing,
                recommendations,
                risks,
                evidence,
                cvVariants,
                letterVariants,
                mail,
                recommendedCommands,
                explanation
        );
    }

    @Transactional(readOnly = true)
    public List<CvVariantProposalDto> generateCvVariants(Long ownerId, Long applicationId, Long versionId) {
        return analyzeSmart(ownerId, applicationId, versionId).cvVariants();
    }

    @Transactional(readOnly = true)
    public List<LetterVariantProposalDto> generateLetterVariants(Long ownerId, Long applicationId, Long versionId) {
        return analyzeSmart(ownerId, applicationId, versionId).letterVariants();
    }

    public SmartApplicationPackResponse exportSmartPack(Long ownerId, Long applicationId, Long versionId) {
        SmartOfferAnalysisResponse analysis = analyzeSmart(ownerId, applicationId, versionId);
        byte[] zipBytes = buildSmartPackZip(analysis);
        StoredFile stored = storageService.storeBytes(
                "portfolio-owner-" + ownerId + "-application-" + applicationId + "-smart-package.zip",
                zipBytes
        );
        return new SmartApplicationPackResponse(true, publicUrl(stored), "Package candidature intelligent généré.", ownerId, applicationId);
    }

    private StructuredOfferDto structureOffer(JobApplication application) {
        String text = clean(application.getOfferText());
        String haystack = normalize(application.getCompanyName() + " " + application.getRoleTitle() + " " + text);
        List<String> hardSkills = SKILL_RULES.stream()
                .filter(rule -> containsAny(haystack, rule.aliases()))
                .map(SkillRule::label)
                .distinct()
                .toList();
        List<String> softSkills = SOFT_SKILL_RULES.stream()
                .filter(skill -> haystack.contains(normalize(skill)))
                .distinct()
                .toList();
        List<String> missions = extractMissions(text);
        String sector = inferSector(haystack, application.getCompanyName());
        String tone = inferTone(sector, haystack);
        String contract = inferContract(haystack);
        String seniority = inferSeniority(haystack);
        List<String> mustHave = hardSkills.stream().limit(Math.min(6, hardSkills.size())).toList();
        List<String> niceToHave = hardSkills.stream().skip(Math.min(6, hardSkills.size())).limit(6).toList();
        List<String> implicit = inferImplicitExpectations(haystack, sector, hardSkills);
        List<String> ats = buildAtsKeywords(hardSkills, softSkills, missions, implicit);

        return new StructuredOfferDto(
                nonBlank(application.getCompanyName(), inferCompanyName(text)),
                sector,
                tone,
                nonBlank(application.getRoleTitle(), inferRoleTitle(text)),
                contract,
                clean(application.getLocation()),
                seniority,
                hardSkills,
                softSkills,
                missions,
                mustHave,
                niceToHave,
                implicit,
                ats
        );
    }

    private List<CandidateEvidenceDto> buildEvidence(WebsiteVersion version, StructuredOfferDto offer) {
        List<CandidateEvidenceDto> evidence = new ArrayList<>();
        Profile profile = version.getProfile();
        if (profile != null) {
            String text = clean(profile.getTitle()) + " " + clean(profile.getHeadline()) + " " + clean(profile.getDescription()) + " " + clean(profile.getShortDescription());
            List<String> tags = extractKnownSkills(text);
            int score = evidenceScore(text, tags, offer);
            evidence.add(new CandidateEvidenceDto(
                    "profile-" + profile.getId(),
                    "PROFILE",
                    nonBlank(profile.getTitle(), "Profil"),
                    nonBlank(profile.getSubtitle(), profile.getAvailability()),
                    score,
                    matchedEvidenceKeywords(text, offer),
                    tags,
                    shorten(nonBlank(profile.getHeadline(), profile.getDescription()), 320),
                    shorten(nonBlank(profile.getHeadline(), profile.getDescription()), 180),
                    "Mon profil combine " + joinNatural(tags.stream().limit(4).toList()) + " avec une approche orientée qualité et impact utilisateur.",
                    score >= 45
            ));
        }

        Timeline timeline = version.getTimeline();
        if (timeline != null && timeline.getExperiences() != null) {
            for (Experience exp : timeline.getExperiences()) {
                String text = clean(exp.getTitle()) + " " + clean(exp.getOrganization()) + " " + clean(exp.getSummary()) + " " + clean(exp.getDescription()) + " " + String.join(" ", safeList(exp.getSkills()));
                List<String> tags = mergeUnique(safeList(exp.getSkills()), extractKnownSkills(text));
                int score = evidenceScore(text, tags, offer) + categoryBonus(clean(exp.getCategory() == null ? null : exp.getCategory().name()), offer);
                evidence.add(new CandidateEvidenceDto(
                        "experience-" + exp.getId(),
                        "EXPERIENCE",
                        nonBlank(exp.getTitle(), "Expérience"),
                        nonBlank(exp.getOrganization(), exp.getLocation()),
                        Math.min(100, score),
                        matchedEvidenceKeywords(text, offer),
                        tags,
                        shorten(nonBlank(exp.getSummary(), exp.getDescription()), 360),
                        buildEvidenceBullet(exp.getTitle(), tags, offer),
                        buildLetterSentence(exp.getTitle(), exp.getOrganization(), tags, offer),
                        score >= 40
                ));
            }
        }

        if (version.getProjects() != null) {
            for (Project project : version.getProjects()) {
                if (Boolean.FALSE.equals(project.getPublished())) continue;
                String text = clean(project.getTitle()) + " " + clean(project.getSubtitle()) + " " + clean(project.getShortDescription()) + " " + clean(project.getDescription()) + " " + String.join(" ", safeList(project.getStacks())) + " " + String.join(" ", safeList(project.getFeatures()));
                List<String> tags = mergeUnique(safeList(project.getStacks()), extractKnownSkills(text));
                int score = evidenceScore(text, tags, offer) + (Boolean.TRUE.equals(project.getFeatured()) ? 8 : 0);
                evidence.add(new CandidateEvidenceDto(
                        "project-" + project.getId(),
                        "PROJECT",
                        nonBlank(project.getTitle(), "Projet"),
                        nonBlank(project.getSubtitle(), String.join(", ", tags.stream().limit(4).toList())),
                        Math.min(100, score),
                        matchedEvidenceKeywords(text, offer),
                        tags,
                        shorten(nonBlank(project.getShortDescription(), project.getDescription()), 360),
                        buildEvidenceBullet(project.getTitle(), tags, offer),
                        buildLetterSentence(project.getTitle(), "projet", tags, offer),
                        score >= 42
                ));
            }
        }

        evidence.sort(Comparator.comparingInt(CandidateEvidenceDto::score).reversed());
        return evidence;
    }

    private MatchingScoreDto score(StructuredOfferDto offer, List<CandidateEvidenceDto> evidence) {
        Set<String> evidenceKeywords = new LinkedHashSet<>();
        for (CandidateEvidenceDto item : evidence) {
            evidenceKeywords.addAll(normalizedList(item.evidenceTags()));
            evidenceKeywords.addAll(normalizedList(item.matchedKeywords()));
        }
        int hardMatched = countMatched(offer.hardSkills(), evidenceKeywords);
        int hardTotal = Math.max(1, offer.hardSkills().size());
        int hardScore = clamp((int) Math.round((hardMatched * 100.0) / hardTotal));
        int softMatched = countMatched(offer.softSkills(), evidenceKeywords);
        int softScore = offer.softSkills().isEmpty() ? 70 : clamp((int) Math.round((softMatched * 100.0) / Math.max(1, offer.softSkills().size())) + 30);
        int missionScore = clamp(45 + Math.min(45, evidence.stream().mapToInt(item -> item.matchedKeywords().size()).sum() * 4));
        int evidenceScore = clamp((int) Math.round(evidence.stream().limit(5).mapToInt(CandidateEvidenceDto::score).average().orElse(45)));
        int atsScore = clamp(35 + countMatched(offer.atsKeywords(), evidenceKeywords) * 6);
        int riskScore = clamp(100 - Math.max(0, offer.mustHave().size() - hardMatched) * 14);
        int global = clamp((int) Math.round(hardScore * 0.32 + evidenceScore * 0.26 + atsScore * 0.18 + missionScore * 0.14 + softScore * 0.10));
        return new MatchingScoreDto(global, hardScore, softScore, missionScore, evidenceScore, atsScore, riskScore);
    }

    private List<CvVariantProposalDto> buildCvVariants(JobApplication application, StructuredOfferDto offer, List<CandidateEvidenceDto> evidence, List<String> matched, List<String> missing) {
        List<CandidateEvidenceDto> topEvidence = evidence.stream().filter(CandidateEvidenceDto::recommended).limit(5).toList();
        List<String> topIds = topEvidence.stream().map(CandidateEvidenceDto::id).toList();
        List<String> keywords = matched.stream().limit(10).toList();
        String target = targetTitle(offer, matched);
        String headline = headlineFor(offer, topEvidence, matched);
        List<String> reductions = List.of("Réduire les projets moins alignés", "Limiter les descriptions longues", "Conserver 2 expériences et 4 projets", "Prioriser les mots-clés ATS détectés");

        return List.of(
                new CvVariantProposalDto(
                        "cv-strict-match",
                        "CV ciblé strict",
                        "Maximise l’alignement avec les compétences obligatoires de l’offre.",
                        variantScore(94, missing.size()),
                        target,
                        headline,
                        keywords,
                        topIds,
                        reductions,
                        commands(target, headline, keywords, 2, 4, "compact", "strict"),
                        List.of(
                                "Titre réécrit pour reprendre le poste cible.",
                                "Compétences classées selon l’offre.",
                                "Preuves les plus proches remontées en priorité."
                        )
                ),
                new CvVariantProposalDto(
                        "cv-balanced",
                        "CV équilibré",
                        "Garde une lecture large du profil tout en renforçant les mots-clés importants.",
                        variantScore(88, Math.max(0, missing.size() - 2)),
                        target,
                        headline,
                        keywords.stream().limit(8).toList(),
                        topIds.stream().limit(4).toList(),
                        List.of("Réduire uniquement les contenus non pertinents", "Garder une variété projet / expérience"),
                        commands(target, headline, keywords.stream().limit(8).toList(), 2, 4, "compact", "balanced"),
                        List.of("Version recommandée si l’offre reste généraliste.", "Bon compromis entre ATS et crédibilité humaine.")
                ),
                new CvVariantProposalDto(
                        "cv-technical-deep",
                        "CV très technique",
                        "Accent sur stack, architecture, backend, data et déploiement.",
                        variantScore(86, missing.size()),
                        target.replace("Alternance", "Alternance technique"),
                        headline + " Mise en avant des choix d’architecture, de la qualité et de l’industrialisation.",
                        keywords,
                        topIds,
                        List.of("Réduire les soft skills visibles", "Détailler davantage les réalisations techniques"),
                        commands(target.replace("Alternance", "Alternance technique"), headline, keywords, 2, 4, "detailed", "technical"),
                        List.of("Adaptée aux recruteurs techniques.", "Expose davantage les preuves concrètes.")
                ),
                new CvVariantProposalDto(
                        "cv-ats",
                        "CV ATS mots-clés",
                        "Optimise les libellés pour retrouver les termes exacts de l’offre.",
                        variantScore(84, missing.size()),
                        target,
                        headlineForAts(offer, matched),
                        mergeUnique(offer.atsKeywords(), keywords).stream().limit(12).toList(),
                        topIds,
                        List.of("Formulations plus explicites", "Moins de synonymes, plus de mots-clés exacts"),
                        commands(target, headlineForAts(offer, matched), mergeUnique(offer.atsKeywords(), keywords).stream().limit(12).toList(), 2, 4, "compact", "ats"),
                        List.of("Reprend les termes exacts de l’offre.", "Utile pour plateformes de candidature et RH.")
                ),
                new CvVariantProposalDto(
                        "cv-one-page-impact",
                        "CV compact une page",
                        "Version lisible, rapide et dense pour candidatures nombreuses.",
                        variantScore(80, missing.size()),
                        target,
                        shorten(headline, 210),
                        keywords.stream().limit(7).toList(),
                        topIds.stream().limit(4).toList(),
                        List.of("2 expériences maximum", "4 projets maximum", "Bullets courts", "Descriptions compactées"),
                        commands(target, shorten(headline, 210), keywords.stream().limit(7).toList(), 2, 4, "compact", "one-page"),
                        List.of("Conserve le modèle une page.", "Réduit le risque de débordement LaTeX.")
                )
        );
    }

    private List<LetterVariantProposalDto> buildLetterVariants(WebsiteVersion version, JobApplication application, StructuredOfferDto offer, List<CandidateEvidenceDto> evidence, List<String> matched) {
        List<CandidateEvidenceDto> top = evidence.stream().filter(CandidateEvidenceDto::recommended).limit(4).toList();
        List<LetterTemplateResponse> selectedTemplates = BUILT_IN_TEMPLATES.stream()
                .sorted(Comparator.comparingInt((LetterTemplateResponse template) -> templateScore(template, offer, matched)).reversed())
                .limit(12)
                .toList();
        List<LetterVariantProposalDto> variants = new ArrayList<>();
        for (LetterTemplateResponse template : selectedTemplates) {
            int score = clamp(templateScore(template, offer, matched) + (top.isEmpty() ? 0 : 12));
            String plain = buildLetterPlainText(version, application, offer, top, template);
            String latex = buildLetterLatex(version, application, offer, top, template, plain);
            variants.add(new LetterVariantProposalDto(
                    "letter-" + template.id(),
                    template.id(),
                    template.name(),
                    template.angle(),
                    template.tone(),
                    score,
                    template.technicalLevel(),
                    plain,
                    latex,
                    top.stream().map(CandidateEvidenceDto::id).toList(),
                    matched.stream().limit(10).toList(),
                    List.of("Angle adapté : " + template.angle(), "Preuves utilisées : " + top.stream().map(CandidateEvidenceDto::title).limit(3).reduce((a, b) -> a + ", " + b).orElse("profil général")),
                    cautionsForTemplate(template, offer)
            ));
        }
        return variants;
    }

    private MailVariantProposalDto buildMailVariant(JobApplication application, StructuredOfferDto offer, List<CandidateEvidenceDto> evidence) {
        String subject = "Candidature — " + nonBlank(offer.roleTitle(), application.getRoleTitle()) + " — Idris ACHABOU";
        String proof = evidence.stream().filter(CandidateEvidenceDto::recommended).findFirst().map(CandidateEvidenceDto::title).orElse("mon portfolio technique");
        String body = "Bonjour,\n\n"
                + "Je me permets de vous transmettre ma candidature pour le poste de " + nonBlank(offer.roleTitle(), application.getRoleTitle())
                + " au sein de " + nonBlank(offer.companyName(), application.getCompanyName()) + ".\n\n"
                + "Mon profil en développement logiciel s’appuie notamment sur " + proof
                + ", avec une approche orientée qualité, architecture et impact utilisateur.\n\n"
                + "Vous trouverez ci-joint mon CV ainsi que ma lettre de motivation. Je reste disponible pour échanger sur l’opportunité.\n\n"
                + "Cordialement,\nIdris ACHABOU";
        return new MailVariantProposalDto(subject, body, 86, List.of("Objet explicite", "Mail court", "Preuve principale citée", "Compatible candidature RH"));
    }

    private byte[] buildSmartPackZip(SmartOfferAnalysisResponse analysis) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            String analysisJson = toJson(analysis);
            writeZipEntry(zip, "analyse-offre-smart.json", analysisJson.getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "cv-variants.json", toJson(analysis.cvVariants()).getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "mail-candidature.txt", (analysis.mail().subject() + "\n\n" + analysis.mail().body()).getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zip, "recommandations.txt", String.join("\n", analysis.recommendations()).getBytes(StandardCharsets.UTF_8));
            for (LetterVariantProposalDto letter : analysis.letterVariants()) {
                String safe = safeFilePart(letter.id());
                writeZipEntry(zip, "letters/" + safe + ".tex", letter.latexSource().getBytes(StandardCharsets.UTF_8));
                writeZipEntry(zip, "letters/" + safe + ".txt", letter.plainText().getBytes(StandardCharsets.UTF_8));
            }
            if (!analysis.letterVariants().isEmpty()) {
                LetterVariantProposalDto best = analysis.letterVariants().get(0);
                CompiledLatex compiled = latexCompileService.compile(best.latexSource(), List.of());
                writeZipEntry(zip, "letters/recommended-compile.log", clean(compiled.logs()).getBytes(StandardCharsets.UTF_8));
                if (compiled.success()) {
                    writeZipEntry(zip, "letters/recommended-letter.pdf", compiled.pdfBytes());
                }
            }
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException exception) {
            return ("Export ZIP intelligent impossible : " + exception.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }

    private List<ApplicationCommandDto> commands(String target, String headline, List<String> keywords, int experienceLimit, int projectLimit, String density, String mode) {
        List<ApplicationCommandDto> commands = new ArrayList<>();
        commands.add(new ApplicationCommandDto("SET_TITLE", "profile.title", target));
        commands.add(new ApplicationCommandDto("SET_HEADLINE", "profile.headline", headline));
        commands.add(new ApplicationCommandDto("LIMIT_EXPERIENCES", "settings.experienceLimit", String.valueOf(experienceLimit)));
        commands.add(new ApplicationCommandDto("LIMIT_PROJECTS", "settings.projectLimit", String.valueOf(projectLimit)));
        commands.add(new ApplicationCommandDto("SET_DENSITY", "settings.density", density));
        commands.add(new ApplicationCommandDto("SET_APPLICATION_MODE", "settings.applicationMode", mode));
        if (!keywords.isEmpty()) {
            commands.add(new ApplicationCommandDto("PRIORITIZE_SKILLS", "skills", String.join(", ", keywords)));
        }
        return commands;
    }

    private String buildLetterPlainText(WebsiteVersion version, JobApplication application, StructuredOfferDto offer, List<CandidateEvidenceDto> top, LetterTemplateResponse template) {
        Owner owner = version.getOwner();
        Profile profile = version.getProfile();
        String fullName = owner == null ? "Idris ACHABOU" : nonBlank(clean(owner.getFirstName()) + " " + clean(owner.getName()), "Idris ACHABOU");
        String company = nonBlank(offer.companyName(), application.getCompanyName());
        String role = nonBlank(offer.roleTitle(), application.getRoleTitle());
        String formation = nonBlank(profile == null ? null : profile.getSubtitle(), "Master Informatique - parcours Science et Technologie du Logiciel à Sorbonne Université");
        String availability = nonBlank(profile == null ? null : profile.getAvailability(), "une alternance à partir de septembre 2026");
        String skills = nonBlank(joinNatural(mergeUnique(offer.hardSkills(), template.bestFor()).stream().limit(6).toList()), "développement logiciel, qualité, architecture et travail en équipe");
        String sector = nonBlank(offer.sector(), "environnement logiciel exigeant");
        String mission = missionFocus(offer);
        String firstEvidence = top.isEmpty() ? "mon projet de portfolio professionnel et mes réalisations académiques" : top.get(0).title();
        String secondEvidence = top.size() > 1 ? top.get(1).title() : "mes projets de développement logiciel";
        String thirdEvidence = top.size() > 2 ? top.get(2).title() : "ma formation en science et technologie du logiciel";
        String firstTags = top.isEmpty() ? skills : joinNatural(top.get(0).evidenceTags().stream().limit(4).toList());
        String secondTags = top.size() > 1 ? joinNatural(top.get(1).evidenceTags().stream().limit(4).toList()) : skills;

        String opening = "Actuellement étudiant en " + formation + ", je recherche " + availability + ". Votre offre de " + role
                + " a retenu mon attention parce qu’elle réunit " + skills + " dans un contexte " + sector
                + ". Ce qui m’intéresse particulièrement est la possibilité de contribuer à " + mission
                + ", tout en progressant dans un cadre où la rigueur, la maintenabilité et la qualité d’exécution ont un impact direct.";

        String proofOne = "Lors de " + firstEvidence + ", j’ai consolidé une approche très concrète du développement : comprendre le besoin, structurer une solution lisible, produire un code exploitable et documenter les choix importants."
                + " Cette expérience mobilise notamment " + nonBlank(firstTags, skills)
                + " et m’a appris à raisonner autant sur la robustesse technique que sur l’usage réel du livrable.";

        String proofTwo = "Mes autres réalisations, en particulier " + secondEvidence + " et " + thirdEvidence
                + ", complètent ce socle par des problématiques d’architecture, de tests, d’intégration et de performance."
                + " Elles me permettent de relier les attentes de votre offre à des preuves précises : modélisation, persistance de données, services applicatifs, interface utilisateur, automatisation et attention portée à la qualité logicielle.";

        String contribution = contributionParagraph(template, offer, company, role, skills, secondTags);
        String closing = "Je souhaite rejoindre " + company + " pour m’investir dans une alternance exigeante, utile et formatrice."
                + " Mon objectif est d’apporter une contribution sérieuse dès les premières missions, de progresser au contact d’une équipe expérimentée et de construire progressivement une autonomie solide sur vos outils, vos méthodes et vos contraintes métier."
                + " Je serais heureux de pouvoir échanger avec vous lors d’un entretien afin de vous présenter plus précisément mon parcours, mes projets et ma motivation.";

        return "Objet : Candidature pour " + role + " — " + company + "\n\n"
                + "Madame, Monsieur,\n\n"
                + opening + "\n\n"
                + proofOne + "\n\n"
                + proofTwo + "\n\n"
                + contribution + "\n\n"
                + closing + "\n\n"
                + rhythmPlainText() + "\n\n"
                + "Je vous prie d’agréer, Madame, Monsieur, l’expression de mes salutations distinguées.\n\n"
                + fullName;
    }

    private String buildLetterLatex(WebsiteVersion version, JobApplication application, StructuredOfferDto offer, List<CandidateEvidenceDto> top, LetterTemplateResponse template, String plainText) {
        Owner owner = version.getOwner();
        Profile profile = version.getProfile();
        String fullName = owner == null ? "Idris ACHABOU" : nonBlank(clean(owner.getFirstName()) + " " + clean(owner.getName()), "Idris ACHABOU");
        String location = nonBlank(profile == null ? null : profile.getLocation(), owner == null ? "" : owner.getAddress());
        String email = contact(owner, "EMAIL");
        String phone = contact(owner, "PHONE_NUMBER", "PHONE");
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE));
        String role = nonBlank(offer.roleTitle(), application.getRoleTitle());
        String company = nonBlank(offer.companyName(), application.getCompanyName());
        String[] paragraphs = letterParagraphsWithoutMeta(plainText);
        StringBuilder latexParagraphs = new StringBuilder();
        for (String paragraph : paragraphs) {
            String p = paragraph.trim();
            if (p.isBlank()) continue;
            if (p.startsWith("Rythme d’alternance") || p.startsWith("Rythme d'alternance")) {
                latexParagraphs.append("\\vspace{0.16cm}\n");
                latexParagraphs.append("{\\small\\textbf{Rythme d’alternance :} Périodes longues (« gros grain ») — Sept.–Oct. : cours et examens ; Nov.–Janv. : entreprise à 100\\% ; Fév.–Mars : cours ; Avril–Sept. : entreprise à 100\\%.}\\par\n");
                continue;
            }
            if (p.startsWith("Je vous prie")) {
                latexParagraphs.append("\\vspace{0.16cm}\n");
            }
            latexParagraphs.append(escapeLatex(p)).append("\\par\n");
            latexParagraphs.append("\\vspace{0.22cm}\n");
        }

        return """
                \\documentclass[10.7pt,a4paper]{article}
                \\usepackage[a4paper,margin=1.45cm]{geometry}
                \\usepackage[T1]{fontenc}
                \\usepackage[utf8]{inputenc}
                \\usepackage[french]{babel}
                \\usepackage[scaled=0.97]{helvet}
                \\renewcommand{\\familydefault}{\\sfdefault}
                \\usepackage[protrusion=true,expansion=false]{microtype}
                \\usepackage{graphicx}
                \\usepackage{tabularx}
                \\usepackage{xcolor}
                \\usepackage{hyperref}
                \\pagestyle{empty}
                \\setlength{\\parindent}{0pt}
                \\setlength{\\parskip}{0pt}
                \\definecolor{primary}{HTML}{5F7F75}
                \\definecolor{ink}{HTML}{172026}
                \\definecolor{muted}{HTML}{5E6B70}
                \\hypersetup{colorlinks=true,urlcolor=primary,linkcolor=primary}
                \\begin{document}
                \\begin{tabularx}{\\linewidth}{@{}X r@{}}
                \\textbf{%s} & \\textbf{%s} \\\\
                \\textbf{%s} & \\\\
                \\textbf{Téléphone : %s} & \\\\
                \\textbf{Email : %s} & \\\\
                \\end{tabularx}
                
                \\vspace{0.52cm}
                {\\Large\\bfseries\\color{ink} Objet : Candidature -- %s}\\par
                {\\small\\color{muted} Variante : %s · Angle : %s · Ton : %s}\\par
                
                \\vspace{0.52cm}
                %s
                \\vspace{0.05cm}
                \\noindent\\rule{0.92\\linewidth}{0.35pt}
                \\vspace{0.18cm}
                \\begin{tabularx}{\\linewidth}{@{}X r@{}}
                {\\Large %s} & \\IfFileExists{signature.png}{\\includegraphics[width=2.75cm]{signature.png}}{} \\\\
                \\end{tabularx}
                \\end{document}
                """.formatted(
                escapeLatex(fullName),
                escapeLatex(today),
                escapeLatex(nonBlank(location, "Choisy-Le-Roi, 94600")),
                escapeLatex(nonBlank(phone, "07 44 75 85 10")),
                escapeLatex(nonBlank(email, "achabou02idris@gmail.com")),
                escapeLatex(role),
                escapeLatex(template.name()),
                escapeLatex(template.angle()),
                escapeLatex(template.tone()),
                latexParagraphs,
                escapeLatex(fullName)
        );
    }

    private String[] letterParagraphsWithoutMeta(String plainText) {
        String normalized = clean(plainText).replace("\r", "");
        List<String> values = new ArrayList<>();
        for (String paragraph : normalized.split("\\n\\n")) {
            String p = paragraph.replace("\n", " ").trim();
            if (p.isBlank() || p.startsWith("Objet :") || p.equals("Madame, Monsieur,")) continue;
            if (p.equals("Idris ACHABOU") || p.equals("Idris Achabou")) continue;
            values.add(p);
        }
        return values.toArray(new String[0]);
    }

    private String contributionParagraph(LetterTemplateResponse template, StructuredOfferDto offer, String company, String role, String skills, String evidenceTags) {
        String category = template.category();
        String mission = missionFocus(offer);
        return switch (category) {
            case "INDUSTRIAL" -> "Votre contexte demande une attention particulière à la fiabilité, à la documentation, aux tests et à la stabilité des évolutions. C’est précisément ce type d’environnement qui m’attire : je veux apprendre à développer dans un cadre industriel structuré, où chaque choix technique doit rester compréhensible, contrôlable et maintenable dans la durée.";
            case "DATA" -> "La dimension données de l’offre m’intéresse particulièrement : structuration, fiabilité des traitements, cohérence des modèles et exploitation par des utilisateurs. Je peux apporter une base solide en " + skills + ", avec l’envie de renforcer ma maîtrise des pipelines, de la persistance et de la qualité des données.";
            case "DEVOPS" -> "L’aspect plateforme et industrialisation constitue pour moi un axe de progression important. Docker, l’automatisation, la reproductibilité des environnements et la qualité des déploiements donnent une vraie valeur aux développements ; je souhaite justement contribuer à des livrables qui ne se limitent pas au code, mais qui restent exécutables, testables et maintenables.";
            case "FULLSTACK" -> "Ce poste m’intéresse aussi parce qu’il relie la logique backend, l’interface et l’usage final. J’aime construire une fonctionnalité de bout en bout : comprendre le besoin, concevoir l’API ou le modèle, produire l’écran utile, puis vérifier que l’ensemble reste clair pour l’utilisateur comme pour l’équipe technique.";
            case "ATS" -> "Votre offre fait ressortir des attentes précises autour de " + skills + ". Je les reprends volontairement car elles correspondent aux axes que je souhaite mettre en avant : développement logiciel, tests, documentation, qualité, architecture et capacité à rejoindre une équipe avec un profil sérieux, adaptable et orienté progression.";
            case "SHORT" -> "La cohérence entre votre besoin et mon parcours tient surtout à la combinaison entre formation logicielle, projets concrets et volonté de progresser dans une équipe exigeante. Je souhaite apporter une contribution fiable, apprendre vite et m’inscrire dans une alternance où les missions confiées ont un impact réel.";
            case "RESEARCH" -> "La dimension méthode, expérimentation et documentation rejoint fortement mon parcours. Je suis à l’aise avec l’idée de comprendre un problème avant de coder, d’expliciter les choix techniques et de transformer progressivement une solution exploratoire en outil stable, lisible et exploitable.";
            case "ESN" -> "Je suis également sensible à la dimension équipe, client et compréhension du besoin. Une alternance dans ce contexte demanderait de s’adapter rapidement, de communiquer clairement et de produire des développements fiables dans un cadre collectif ; ce sont des compétences que je veux renforcer au contact de projets concrets.";
            case "STARTUP" -> "Votre contexte suppose probablement de livrer vite tout en restant rigoureux. C’est une dynamique qui m’intéresse : construire, tester, ajuster, documenter et améliorer sans perdre de vue l’utilisateur final ni la qualité du socle technique.";
            default -> "Cette offre correspond à ce que je recherche pour mon alternance : " + mission + ", développer des fonctionnalités utiles, consolider mes bases techniques et progresser dans un environnement où la qualité du code, la collaboration et la compréhension métier sont essentielles.";
        };
    }

    private String missionFocus(StructuredOfferDto offer) {
        if (!offer.missions().isEmpty()) {
            return shorten(offer.missions().get(0).toLowerCase(Locale.ROOT), 150);
        }
        if (!offer.mustHave().isEmpty()) {
            return "des développements autour de " + joinNatural(offer.mustHave().stream().limit(4).toList());
        }
        return "des applications fiables, maintenables et utiles aux utilisateurs";
    }

    private String rhythmPlainText() {
        return "Rythme d’alternance : périodes longues (« gros grain ») — Septembre à octobre : cours et examens ; novembre à janvier : entreprise à 100 % ; février à mars : cours ; avril à septembre : entreprise à 100 %.";
    }

    private List<String> extractMissions(String text) {
        String[] lines = clean(text).split("\\R|[•·]");
        List<String> missions = new ArrayList<>();
        for (String line : lines) {
            String clean = line.trim();
            String normalized = normalize(clean);
            if (clean.length() < 25 || clean.length() > 220) continue;
            if (normalized.contains("develop") || normalized.contains("concevoir") || normalized.contains("particip") || normalized.contains("mettre en") || normalized.contains("contribu") || normalized.contains("maintenir") || normalized.contains("tester") || normalized.contains("amelior")) {
                missions.add(clean);
            }
            if (missions.size() >= 7) break;
        }
        if (missions.isEmpty() && text.length() > 80) {
            missions.add(shorten(text, 180));
        }
        return missions;
    }

    private List<String> extractKnownSkills(String text) {
        String haystack = normalize(text);
        return SKILL_RULES.stream()
                .filter(rule -> containsAny(haystack, rule.aliases()))
                .map(SkillRule::label)
                .distinct()
                .toList();
    }

    private int evidenceScore(String text, List<String> tags, StructuredOfferDto offer) {
        String haystack = normalize(text + " " + String.join(" ", tags));
        int score = 18;
        for (String skill : offer.hardSkills()) {
            if (haystack.contains(normalize(skill))) score += 13;
        }
        for (String skill : offer.mustHave()) {
            if (haystack.contains(normalize(skill))) score += 8;
        }
        for (String expectation : offer.implicitExpectations()) {
            if (haystack.contains(normalize(expectation))) score += 5;
        }
        return clamp(score);
    }

    private int categoryBonus(String category, StructuredOfferDto offer) {
        String role = normalize(offer.roleTitle());
        String normalizedCategory = normalize(category);
        if (normalizedCategory.contains("internship") || normalizedCategory.contains("stage")) return 8;
        if (role.contains("alternance") && normalizedCategory.contains("alternance")) return 7;
        return 0;
    }

    private List<String> matchedEvidenceKeywords(String text, StructuredOfferDto offer) {
        String haystack = normalize(text);
        return mergeUnique(offer.hardSkills(), offer.implicitExpectations()).stream()
                .filter(keyword -> haystack.contains(normalize(keyword)))
                .limit(10)
                .toList();
    }

    private String buildEvidenceBullet(String title, List<String> tags, StructuredOfferDto offer) {
        String skills = joinNatural(tags.stream().limit(4).toList());
        if (skills.isBlank()) {
            return "Réalisation structurée autour de " + nonBlank(title, "ce projet") + ".";
        }
        return "Mobilisation de " + skills + " dans " + nonBlank(title, "un contexte projet") + ", avec une approche orientée qualité et maintenabilité.";
    }

    private String buildLetterSentence(String title, String context, List<String> tags, StructuredOfferDto offer) {
        String skills = joinNatural(tags.stream().limit(3).toList());
        return "L’expérience " + nonBlank(title, context) + " me permet de relier les attentes de l’offre à une pratique concrète de " + nonBlank(skills, "développement logiciel") + ".";
    }

    private List<String> mergeMatchedKeywords(StructuredOfferDto offer, List<CandidateEvidenceDto> evidence) {
        List<String> values = new ArrayList<>();
        values.addAll(offer.hardSkills());
        values.addAll(offer.softSkills());
        evidence.stream().limit(6).forEach(item -> values.addAll(item.matchedKeywords()));
        return mergeUnique(values, List.of()).stream().limit(16).toList();
    }

    private List<String> missingCriticalKeywords(StructuredOfferDto offer, List<CandidateEvidenceDto> evidence) {
        Set<String> evidenceKeywords = new LinkedHashSet<>();
        evidence.forEach(item -> evidenceKeywords.addAll(normalizedList(item.evidenceTags())));
        return offer.mustHave().stream()
                .filter(keyword -> evidenceKeywords.stream().noneMatch(value -> value.contains(normalize(keyword)) || normalize(keyword).contains(value)))
                .limit(12)
                .toList();
    }

    private List<String> buildSmartRecommendations(StructuredOfferDto offer, MatchingScoreDto scores, List<CandidateEvidenceDto> evidence, List<String> missing) {
        List<String> rec = new ArrayList<>();
        rec.add("Créer au moins 3 documents : CV ciblé strict, lettre technique directe et lettre ATS/RH.");
        if (!evidence.isEmpty()) rec.add("Mettre en avant en priorité : " + evidence.stream().limit(3).map(CandidateEvidenceDto::title).reduce((a, b) -> a + ", " + b).orElse("les preuves principales") + ".");
        if (!offer.mustHave().isEmpty()) rec.add("Reprendre explicitement les mots-clés obligatoires : " + String.join(", ", offer.mustHave()) + ".");
        if (!missing.isEmpty()) rec.add("Traiter les manques sans mentir : " + String.join(", ", missing) + " doivent être reformulés comme notions en cours d’approfondissement si nécessaire.");
        if (scores.atsScore() < 75) rec.add("Renforcer la densité ATS du CV : ajouter les termes exacts de l’offre dans l’accroche et les compétences.");
        if (scores.evidenceScore() < 70) rec.add("Associer chaque compétence importante à une preuve concrète : projet, stage ou réalisation.");
        rec.add("Exporter un pack candidature avec analyse JSON, CV commands, plusieurs lettres et mail prêt à envoyer.");
        return rec;
    }

    private List<String> buildRiskWarnings(JobApplication application, StructuredOfferDto offer, MatchingScoreDto scores, List<CandidateEvidenceDto> evidence) {
        List<String> risks = new ArrayList<>();
        if (clean(application.getOfferText()).length() < 400) risks.add("Offre courte : analyse moins fiable, colle l’annonce complète si possible.");
        if (scores.hardSkillsScore() < 60) risks.add("Plusieurs compétences obligatoires ne sont pas couvertes par les preuves actuelles.");
        if (evidence.stream().noneMatch(item -> item.type().equals("EXPERIENCE") && item.score() > 55)) risks.add("Aucune expérience très alignée détectée : compenser avec projets et réalisations.");
        if (offer.hardSkills().isEmpty()) risks.add("Peu de stack détectée : l’offre semble RH/généraliste, privilégier lettre entreprise + CV équilibré.");
        return risks;
    }

    private String buildExplanation(StructuredOfferDto offer, MatchingScoreDto scores, List<CandidateEvidenceDto> evidence, List<CvVariantProposalDto> cvVariants, List<LetterVariantProposalDto> letterVariants) {
        String topEvidence = evidence.stream().limit(3).map(CandidateEvidenceDto::title).reduce((a, b) -> a + ", " + b).orElse("profil général");
        String topCv = cvVariants.isEmpty() ? "CV ciblé" : cvVariants.get(0).name();
        String topLetter = letterVariants.isEmpty() ? "Lettre technique" : letterVariants.get(0).name();
        return "Analyse : l’offre vise " + offer.roleTitle() + " dans un contexte " + offer.sector() + ". "
                + "Score global " + scores.globalScore() + "/100. Preuves principales : " + topEvidence + ". "
                + "Version recommandée : " + topCv + " + " + topLetter + ".";
    }

    private String targetTitle(StructuredOfferDto offer, List<String> matched) {
        String role = normalize(offer.roleTitle());
        if (role.contains("devops") || matched.contains("Docker") || matched.contains("Kubernetes")) return "Alternance Développeur Full Stack / DevOps";
        if (role.contains("data") || matched.contains("Data pipeline") || matched.contains("Python")) return "Alternance Développeur Data / Backend";
        if (matched.contains("React") || matched.contains("TypeScript")) return "Alternance Développeur Full Stack React / Spring";
        if (matched.contains("Java") || matched.contains("Spring Boot")) return "Alternance Développeur Java Spring Boot";
        return nonBlank(offer.roleTitle(), "Alternance Développement logiciel");
    }

    private String headlineFor(StructuredOfferDto offer, List<CandidateEvidenceDto> evidence, List<String> matched) {
        String skills = joinNatural(matched.stream().limit(5).toList());
        String proof = evidence.isEmpty() ? "projets logiciels structurés" : evidence.get(0).title();
        return "Profil rigoureux orienté " + nonBlank(skills, "développement logiciel") + ", capable de relier besoin métier, architecture claire et livrables maintenables. Preuve principale : " + proof + ".";
    }

    private String headlineForAts(StructuredOfferDto offer, List<String> matched) {
        return "Candidature alignée avec " + nonBlank(offer.roleTitle(), "l’offre") + " : " + String.join(", ", mergeUnique(offer.atsKeywords(), matched).stream().limit(10).toList()) + ".";
    }

    private int templateScore(LetterTemplateResponse template, StructuredOfferDto offer, List<String> matched) {
        int score = 35;
        Set<String> haystack = normalizedSet(mergeUnique(offer.hardSkills(), matched));
        for (String item : template.bestFor()) {
            if (haystack.stream().anyMatch(value -> value.contains(normalize(item)) || normalize(item).contains(value))) score += 12;
        }
        String sector = normalize(offer.sector());
        if (template.category().equals("INDUSTRIAL") && (sector.contains("industrie") || sector.contains("defense"))) score += 16;
        if (template.category().equals("DATA") && haystack.contains("data pipeline")) score += 12;
        if (template.category().equals("DEVOPS") && (haystack.contains("docker") || haystack.contains("kubernetes"))) score += 12;
        if (template.category().equals("ATS")) score += 8;
        if (template.category().equals("SHORT") && offer.hardSkills().size() < 4) score += 8;
        return clamp(score);
    }

    private List<String> cautionsForTemplate(LetterTemplateResponse template, StructuredOfferDto offer) {
        List<String> cautions = new ArrayList<>();
        if (template.technicalLevel() > 85 && offer.hardSkills().size() < 3) cautions.add("Offre peu technique : cette version peut paraître trop stack-oriented.");
        if (template.category().equals("ATS")) cautions.add("À relire pour éviter une lettre trop mécanique.");
        if (template.category().equals("SHORT")) cautions.add("Version efficace mais moins personnalisée.");
        return cautions;
    }

    private List<String> inferImplicitExpectations(String haystack, String sector, List<String> hardSkills) {
        List<String> implicit = new ArrayList<>();
        if (hardSkills.stream().anyMatch(skill -> normalize(skill).contains("spring") || normalize(skill).contains("java"))) {
            implicit.add("maintenabilité");
            implicit.add("architecture");
            implicit.add("qualité logicielle");
        }
        if (hardSkills.stream().anyMatch(skill -> normalize(skill).contains("docker") || normalize(skill).contains("kubernetes"))) {
            implicit.add("industrialisation");
            implicit.add("reproductibilité");
        }
        if (haystack.contains("critique") || normalize(sector).contains("defense") || normalize(sector).contains("industrie")) {
            implicit.add("rigueur");
            implicit.add("fiabilité");
            implicit.add("documentation");
        }
        if (haystack.contains("agile") || haystack.contains("scrum") || haystack.contains("equipe")) {
            implicit.add("collaboration");
            implicit.add("communication");
        }
        return mergeUnique(implicit, List.of()).stream().limit(8).toList();
    }

    private List<String> buildAtsKeywords(List<String> hardSkills, List<String> softSkills, List<String> missions, List<String> implicit) {
        List<String> values = new ArrayList<>();
        values.addAll(hardSkills);
        values.addAll(softSkills);
        values.addAll(implicit);
        for (String mission : missions) {
            values.addAll(extractKnownSkills(mission));
        }
        return mergeUnique(values, List.of()).stream().limit(18).toList();
    }

    private String inferSector(String haystack, String companyName) {
        if (haystack.contains("defense") || haystack.contains("aeronaut") || haystack.contains("spatial") || normalize(companyName).contains("thales") || normalize(companyName).contains("safran") || normalize(companyName).contains("dassault")) return "défense / industrie / systèmes critiques";
        if (haystack.contains("banque") || haystack.contains("finance") || haystack.contains("assurance")) return "banque / finance / assurance";
        if (haystack.contains("sante") || haystack.contains("medical")) return "santé / logiciel réglementé";
        if (haystack.contains("startup") || haystack.contains("scale-up") || haystack.contains("produit")) return "startup / produit numérique";
        if (haystack.contains("client") || haystack.contains("consultant") || haystack.contains("esn")) return "ESN / conseil / client";
        if (haystack.contains("recherche") || haystack.contains("laboratoire")) return "recherche / laboratoire";
        return "logiciel / services numériques";
    }

    private String inferTone(String sector, String haystack) {
        String normalized = normalize(sector + " " + haystack);
        if (normalized.contains("defense") || normalized.contains("industrie") || normalized.contains("banque")) return "formel, rigoureux, technique";
        if (normalized.contains("startup")) return "direct, produit, impact";
        if (normalized.contains("recherche")) return "académique, méthodique";
        return "professionnel, clair, orienté preuves";
    }

    private String inferContract(String haystack) {
        if (haystack.contains("alternance")) return "alternance";
        if (haystack.contains("stage")) return "stage";
        if (haystack.contains("cdi")) return "CDI";
        if (haystack.contains("cdd")) return "CDD";
        return "non précisé";
    }

    private String inferSeniority(String haystack) {
        if (haystack.contains("senior") || haystack.contains("confirme")) return "confirmé";
        if (haystack.contains("junior") || haystack.contains("debutant") || haystack.contains("alternance") || haystack.contains("stage")) return "étudiant / junior";
        return "non précisé";
    }

    private String inferCompanyName(String text) {
        return "Entreprise à confirmer";
    }

    private String inferRoleTitle(String text) {
        String normalized = normalize(text);
        if (normalized.contains("java") && normalized.contains("spring")) return "Développeur Java Spring Boot";
        if (normalized.contains("full stack") || normalized.contains("fullstack")) return "Développeur Full Stack";
        if (normalized.contains("data")) return "Développeur Data / Backend";
        return "Poste à confirmer";
    }

    private JobApplication findApplication(Long ownerId, Long applicationId) {
        return jobApplicationRepository.findByIdAndOwnerId(applicationId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication"));
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

    private boolean containsAny(String haystack, List<String> aliases) {
        return aliases.stream().anyMatch(alias -> haystack.contains(normalize(alias)));
    }

    private int countMatched(List<String> values, Set<String> evidenceKeywords) {
        int count = 0;
        for (String value : values) {
            String n = normalize(value);
            if (evidenceKeywords.stream().anyMatch(keyword -> keyword.contains(n) || n.contains(keyword))) count++;
        }
        return count;
    }

    private Set<String> normalizedSet(List<String> values) {
        return new LinkedHashSet<>(normalizedList(values));
    }

    private List<String> normalizedList(List<String> values) {
        return safeList(values).stream().map(this::normalize).filter(value -> !value.isBlank()).distinct().toList();
    }

    private List<String> mergeUnique(List<String> first, List<String> second) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        safeList(first).stream().filter(value -> !clean(value).isBlank()).forEach(values::add);
        safeList(second).stream().filter(value -> !clean(value).isBlank()).forEach(values::add);
        return new ArrayList<>(values);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    private int variantScore(int base, int missingCount) {
        return clamp(base - Math.min(24, missingCount * 4));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String joinNatural(List<String> values) {
        List<String> cleanValues = safeList(values);
        if (cleanValues.isEmpty()) return "";
        if (cleanValues.size() == 1) return cleanValues.get(0);
        if (cleanValues.size() == 2) return cleanValues.get(0) + " et " + cleanValues.get(1);
        return String.join(", ", cleanValues.subList(0, cleanValues.size() - 1)) + " et " + cleanValues.get(cleanValues.size() - 1);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"JSON generation failed\"}";
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

    private String safeFilePart(String value) {
        String normalized = normalize(value).replaceAll("[^a-z0-9]+", "-");
        return normalized.isBlank() ? "document" : normalized.replaceAll("^-|-$", "");
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

    private record SkillRule(String label, String category, int weight, List<String> aliases) {
    }
}
