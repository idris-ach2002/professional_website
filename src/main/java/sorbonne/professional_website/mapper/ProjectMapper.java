package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ProjectCaseStudyRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.response.ProjectCaseStudyResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.entity.Project;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ProjectMapper {

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
                project.getDocumentationUrl(),
                project.getArchitectureUrl(),
                copyStringList(project.getStacks()),
                copyStringList(project.getFeatures()),
                ProjectLinkMapper.toResponseList(project.getLinks()),
                project.getFeatured(),
                project.getPublished(),
                project.getDisplayOrder(),
                effectiveSlug(project),
                copyStringList(project.getProofTags()),
                toCaseStudyResponse(project)
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
        Set<String> slugs = new HashSet<>();

        for (ProjectRequestDTO projectDTO : projectDTOs) {
            Project project = fromRequest(projectDTO);

            if (project != null) {
                String slug = project.getSlug();
                if (!slugs.add(slug.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Deux projets d'une même version ne peuvent pas partager le même slug.");
                }
                projects.add(project);
            }
        }

        return projects;
    }

    public static String normalizedSlug(ProjectRequestDTO projectDTO) {
        if (projectDTO == null) return "";
        return slugifyValue(defaultIfBlank(projectDTO.slug(), projectDTO.title()));
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
        project.setDocumentationUrl(projectDTO.documentationUrl());
        project.setArchitectureUrl(projectDTO.architectureUrl());
        project.setSlug(normalizedSlug(projectDTO));
        project.setStacks(copyStringList(projectDTO.stacks()));
        project.setFeatures(copyStringList(projectDTO.features()));
        project.setProofTags(copyStringList(projectDTO.proofTags()));
        project.setLinks(ProjectLinkMapper.fromRequestList(projectDTO.links()));
        applyCaseStudy(project, projectDTO.caseStudy());
        project.setFeatured(projectDTO.featured());
        project.setPublished(projectDTO.published());
        project.setDisplayOrder(projectDTO.displayOrder());
    }

    private static void applyCaseStudy(Project project, ProjectCaseStudyRequestDTO caseStudy) {
        if (caseStudy == null) {
            project.setCaseStudyProblem(null);
            project.setCaseStudyContext(null);
            project.setCaseStudyRole(null);
            project.setCaseStudyArchitecture(null);
            project.setCaseStudyNextSteps(null);
            project.setCaseStudyTechnicalChoices(new ArrayList<>());
            project.setCaseStudyChallenges(new ArrayList<>());
            project.setCaseStudySolutions(new ArrayList<>());
            project.setCaseStudyOutcomes(new ArrayList<>());
            project.setCaseStudyResults(new ArrayList<>());
            project.setCaseStudyLimits(new ArrayList<>());
            return;
        }

        project.setCaseStudyProblem(caseStudy.problem());
        project.setCaseStudyContext(caseStudy.context());
        project.setCaseStudyRole(caseStudy.role());
        project.setCaseStudyArchitecture(caseStudy.architecture());
        project.setCaseStudyNextSteps(caseStudy.nextSteps());
        project.setCaseStudyTechnicalChoices(copyStringList(caseStudy.technicalChoices()));
        project.setCaseStudyChallenges(copyStringList(caseStudy.challenges()));
        project.setCaseStudySolutions(copyStringList(caseStudy.solutions()));
        project.setCaseStudyOutcomes(copyStringList(caseStudy.outcomes()));
        project.setCaseStudyResults(copyStringList(caseStudy.results()));
        project.setCaseStudyLimits(copyStringList(caseStudy.limits()));
    }

    private static ProjectCaseStudyResponseDTO toCaseStudyResponse(Project project) {
        boolean empty = isBlank(project.getCaseStudyProblem())
                && isBlank(project.getCaseStudyContext())
                && isBlank(project.getCaseStudyRole())
                && isBlank(project.getCaseStudyArchitecture())
                && isBlank(project.getCaseStudyNextSteps())
                && isEmpty(project.getCaseStudyTechnicalChoices())
                && isEmpty(project.getCaseStudyChallenges())
                && isEmpty(project.getCaseStudySolutions())
                && isEmpty(project.getCaseStudyOutcomes())
                && isEmpty(project.getCaseStudyResults())
                && isEmpty(project.getCaseStudyLimits());
        if (empty) return null;

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

    public static String effectiveSlug(Project project) {
        return defaultIfBlank(project.getSlug(), slugifyValue(project.getTitle()));
    }

    public static String slugifyValue(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100).replaceAll("-+$", "");
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private static List<String> copyStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values);
    }
}
