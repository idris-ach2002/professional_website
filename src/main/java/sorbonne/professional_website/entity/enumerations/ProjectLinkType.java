package sorbonne.professional_website.entity.enumerations;

import lombok.Getter;

@Getter
public enum ProjectLinkType {

    GITHUB("GitHub"),
    DEMO("Démo"),
    DOCUMENTATION("Documentation"),
    ARCHITECTURE("Architecture"),
    FIGMA("Figma"),
    VIDEO("Vidéo"),
    ARTICLE("Article"),
    OTHER("Autre");

    private final String label;

    ProjectLinkType(String label) {
        this.label = label;
    }
}