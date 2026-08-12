package sorbonne.professional_website.service;

import org.springframework.stereotype.Component;
import sorbonne.professional_website.dto.response.PortfolioHealthCheckResponseDTO;
import sorbonne.professional_website.dto.response.PortfolioHealthReportResponseDTO;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class PortfolioHealthEvaluator {

    public PortfolioHealthReportResponseDTO evaluate(Long ownerId, Long versionId, WebsiteVersion version) {
        List<PortfolioHealthCheckResponseDTO> checks = new ArrayList<>();

        Profile profile = version.getProfile();
        Timeline timeline = version.getTimeline();
        List<Project> projects = version.getProjects() == null ? List.of() : version.getProjects();
        List<Experience> experiences = timeline == null || timeline.getExperiences() == null
                ? List.of()
                : timeline.getExperiences();
        Owner owner = version.getOwner();

        addCheck(checks, "profile.title", "Titre du profil", "BLOCKER", !isBlank(profile == null ? null : profile.getTitle()), "Le profil doit avoir un titre public.");
        addCheck(checks, "profile.description", "Description du profil", "BLOCKER", !isBlank(profile == null ? null : profile.getDescription()), "La description publique du profil est obligatoire.");
        addCheck(checks, "profile.cv", "CV attaché", "WARNING", profile != null && !isBlank(profile.getCvUrl()), "Aucun CV n'est attaché à cette version.");
        addCheck(checks, "timeline", "Timeline", "BLOCKER", timeline != null && !experiences.isEmpty(), "La timeline doit contenir au moins une expérience ou formation.");
        addCheck(checks, "projects.published", "Projets publiés", "BLOCKER", projects.stream().anyMatch(project -> project.getPublished() == null || Boolean.TRUE.equals(project.getPublished())), "Au moins un projet publié est nécessaire.");
        addCheck(checks, "projects.featured", "Projet mis en avant", "WARNING", projects.stream().anyMatch(project -> Boolean.TRUE.equals(project.getFeatured())), "Aucun projet n'est marqué comme featured.");
        addCheck(checks, "assets.profile", "Image profil", "SUGGESTION", profile != null && !isBlank(profile.getProfileImageUrl()), "Ajoute une image de profil pour un rendu public plus professionnel.");
        addCheck(checks, "contacts.email", "Contact email", "BLOCKER", owner != null && hasContact(owner, "EMAIL"), "Ajoute un email dans les contacts owner.");
        addCheck(checks, "contacts.github", "Lien GitHub", "WARNING", owner != null && hasContact(owner, "GITHUB"), "Ajoute un lien GitHub dans les contacts owner.");
        addCheck(checks, "links.projects", "Liens projets", "SUGGESTION", projects.stream().anyMatch(project -> !isBlank(project.getGithubUrl()) || !isBlank(project.getDocumentationUrl())), "Ajoute au moins un lien GitHub ou documentation sur les projets publiés.");
        addCheck(checks, "version.published", "Version publiée", "WARNING", Boolean.TRUE.equals(version.getPublished()), "La version n'est pas marquée comme published.");
        addCheck(checks, "version.active", "Version active", "SUGGESTION", Boolean.TRUE.equals(version.getActive()), "La version n'est pas active. Utilise la validation avant publication.");
        addCheck(checks, "dates.experiences", "Dates expériences", "WARNING", experiences.stream().allMatch(experience -> experience.getStartDate() != null), "Certaines expériences n'ont pas de date de début.");
        addCheck(checks, "projects.images", "Images projets", "SUGGESTION", projects.stream().filter(project -> project.getPublished() == null || Boolean.TRUE.equals(project.getPublished())).allMatch(project -> !isBlank(project.getImageUrl())), "Certains projets publiés n'ont pas d'image.");

        long blockers = countFailures(checks, "BLOCKER");
        long warnings = countFailures(checks, "WARNING");
        long suggestions = countFailures(checks, "SUGGESTION");
        int score = Math.max(0, 100 - (int) blockers * 30 - (int) warnings * 8 - (int) suggestions * 3);

        return new PortfolioHealthReportResponseDTO(
                score,
                blockers == 0,
                (int) blockers,
                (int) warnings,
                (int) suggestions,
                List.copyOf(checks),
                LocalDateTime.now(),
                ownerId,
                versionId
        );
    }

    private static long countFailures(List<PortfolioHealthCheckResponseDTO> checks, String severity) {
        return checks.stream()
                .filter(check -> severity.equals(check.severity()) && "FAIL".equals(check.status()))
                .count();
    }

    private static void addCheck(List<PortfolioHealthCheckResponseDTO> checks, String id, String label, String severity, boolean pass, String failureMessage) {
        checks.add(new PortfolioHealthCheckResponseDTO(
                id, label, severity, pass ? "PASS" : "FAIL", pass ? "OK" : failureMessage
        ));
    }

    private static boolean hasContact(Owner owner, String type) {
        if (owner.getContacts() == null) return false;
        return owner.getContacts().stream()
                .filter(Objects::nonNull)
                .anyMatch(contact -> contact.getType() != null
                        && type.equals(contact.getType().name())
                        && !isBlank(contact.getValue()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
