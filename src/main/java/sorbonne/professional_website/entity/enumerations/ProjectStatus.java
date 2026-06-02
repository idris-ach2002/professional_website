package sorbonne.professional_website.entity.enumerations;

import lombok.Getter;

@Getter
public enum ProjectStatus {

    PLANNED("Prévu"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminé"),
    MAINTAINED("Maintenu"),
    ARCHIVED("Archivé");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }
}