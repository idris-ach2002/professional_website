package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ProjectCaseStudyRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.response.ProjectCaseStudyResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.entity.Project;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ProjectMapper {

    private static final Pattern NON_LATIN_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private ProjectMapper() {
    }

    public static ProjectResponseDTO toResponse(Project project) {
        if (project == null) {
            return null;
        }

        return new ProjectResponseDTO(
                project.getId(),
                project.getTitle(),
                project.getSubtitle(),
                project.getShortDescription(),
                project.getDescription(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getImageUrl(),
                project.getDemoUrl(),
                project.getGithubUrl(),
                project.getArchitectureUrl(),
                project.getDocumentationUrl(),
                defaultIfBlank(project.getSlug(), slugify(project.getTitle())),
                copyStringList(project.getStacks()),
                copyStringList(project.getFeatures()),
                ProjectLinkMapper.toResponseList(project.getLinks()),
                copyStringList(project.getProofTags()),
                toCaseStudyResponse(project),
                project.getFeatured(),
                project.getPublished(),
                project.getDisplayOrder()
        );
    }

    public static Project fromRequest(ProjectRequestDTO projectDTO) {
        if (projectDTO == null) {
            return null;
        }

        Project project = new Project();
        setProjectProperties(project, projectDTO);

        return project;
    }

    public static void updateEntityFromRequest(Project project, ProjectRequestDTO projectDTO) {
        if (project == null || projectDTO == null) {
            return;
        }

        setProjectProperties(project, projectDTO);
    }

    public static List<ProjectResponseDTO> toResponseList(List<Project> projects) {
        if (projects == null) {
            return List.of();
        }

        List<ProjectResponseDTO> projectDTOs = new ArrayList<>();

        for (Project project : projects) {
            projectDTOs.add(toResponse(project));
        }

        return projectDTOs;
    }

    public static List<Project> fromRequestList(List<ProjectRequestDTO> projectDTOs) {
        if (projectDTOs == null) {
            return new ArrayList<>();
        }

        List<Project> projects = new ArrayList<>();

        for (ProjectRequestDTO projectDTO : projectDTOs) {
            Project project = fromRequest(projectDTO);

            if (project != null) {
                projects.add(project);
            }
        }

        return projects;
    }

    public static String slugify(String value) {
        if (isBlank(value)) {
            return "projet";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = NON_LATIN_MARKS.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_SLUG_CHARS.matcher(normalized).replaceAll("-");
        normalized = EDGE_DASHES.matcher(normalized).replaceAll("");

        return normalized.isBlank() ? "projet" : normalized;
    }

    private static void setProjectProperties(Project project, ProjectRequestDTO projectDTO) {
        project.setTitle(projectDTO.title());
        project.setSubtitle(projectDTO.subtitle());
        project.setShortDescription(projectDTO.shortDescription());
        project.setDescription(projectDTO.description());
        project.setStatus(projectDTO.status());
        project.setStartDate(projectDTO.startDate());
        project.setEndDate(projectDTO.endDate());
        project.setImageUrl(projectDTO.imageUrl());
        project.setDemoUrl(projectDTO.demoUrl());
        project.setGithubUrl(projectDTO.githubUrl());
        project.setArchitectureUrl(projectDTO.architectureUrl());
        project.setDocumentationUrl(projectDTO.documentationUrl());
        project.setSlug(defaultIfBlank(projectDTO.slug(), slugify(projectDTO.title())));
        project.setStacks(copyStringList(projectDTO.stacks()));
        project.setFeatures(copyStringList(projectDTO.features()));
        project.setLinks(ProjectLinkMapper.fromRequestList(projectDTO.links()));
        project.setProofTags(copyStringList(projectDTO.proofTags()));
        applyCaseStudy(project, projectDTO.caseStudy());
        project.setFeatured(projectDTO.featured());
        project.setPublished(projectDTO.published());
        project.setDisplayOrder(projectDTO.displayOrder());
    }

    private static void applyCaseStudy(Project project, ProjectCaseStudyRequestDTO caseStudy) {
        project.setCaseStudyProblem(caseStudy == null ? null : caseStudy.problem());
        project.setCaseStudyContext(caseStudy == null ? null : caseStudy.context());
        project.setCaseStudyRole(caseStudy == null ? null : caseStudy.role());
        project.setCaseStudyArchitecture(caseStudy == null ? null : caseStudy.architecture());
        project.setCaseStudyTechnicalChoices(copyStringList(caseStudy == null ? null : caseStudy.technicalChoices()));
        project.setCaseStudyChallenges(copyStringList(caseStudy == null ? null : caseStudy.challenges()));
        project.setCaseStudySolutions(copyStringList(caseStudy == null ? null : caseStudy.solutions()));
        project.setCaseStudyOutcomes(copyStringList(caseStudy == null ? null : caseStudy.outcomes()));
        project.setCaseStudyResults(copyStringList(caseStudy == null ? null : caseStudy.results()));
        project.setCaseStudyLimits(copyStringList(caseStudy == null ? null : caseStudy.limits()));
        project.setCaseStudyNextSteps(caseStudy == null ? null : caseStudy.nextSteps());
    }

    public static ProjectCaseStudyResponseDTO toCaseStudyResponse(Project project) {
        if (project == null || !hasCaseStudy(project)) {
            return null;
        }

        return new ProjectCaseStudyResponseDTO(
                project.getCaseStudyProblem(),
                project.getCaseStudyContext(),
                project.getCaseStudyRole(),
                project.getCaseStudyArchitecture(),
                copyStringList(project.getCaseStudyTechnicalChoices()),
                copyStringList(project.getCaseStudyChallenges()),
                copyStringList(project.getCaseStudySolutions()),
                copyStringList(project.getCaseStudyOutcomes()),
                copyStringList(project.getCaseStudyResults()),
                copyStringList(project.getCaseStudyLimits()),
                project.getCaseStudyNextSteps()
        );
    }

    public static ProjectCaseStudyRequestDTO toCaseStudyRequest(Project project) {
        if (project == null || !hasCaseStudy(project)) {
            return null;
        }

        return new ProjectCaseStudyRequestDTO(
                project.getCaseStudyProblem(),
                project.getCaseStudyContext(),
                project.getCaseStudyRole(),
                project.getCaseStudyArchitecture(),
                copyStringList(project.getCaseStudyTechnicalChoices()),
                copyStringList(project.getCaseStudyChallenges()),
                copyStringList(project.getCaseStudySolutions()),
                copyStringList(project.getCaseStudyOutcomes()),
                copyStringList(project.getCaseStudyResults()),
                copyStringList(project.getCaseStudyLimits()),
                project.getCaseStudyNextSteps()
        );
    }

    private static boolean hasCaseStudy(Project project) {
        return !isBlank(project.getCaseStudyProblem())
                || !isBlank(project.getCaseStudyContext())
                || !isBlank(project.getCaseStudyRole())
                || !isBlank(project.getCaseStudyArchitecture())
                || !isBlank(project.getCaseStudyNextSteps())
                || !copyStringList(project.getCaseStudyTechnicalChoices()).isEmpty()
                || !copyStringList(project.getCaseStudyChallenges()).isEmpty()
                || !copyStringList(project.getCaseStudySolutions()).isEmpty()
                || !copyStringList(project.getCaseStudyOutcomes()).isEmpty()
                || !copyStringList(project.getCaseStudyResults()).isEmpty()
                || !copyStringList(project.getCaseStudyLimits()).isEmpty();
    }

    private static List<String> copyStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values);
    }

    private static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
