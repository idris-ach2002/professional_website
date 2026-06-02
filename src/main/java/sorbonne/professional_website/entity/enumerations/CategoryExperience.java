package sorbonne.professional_website.entity.enumerations;

import lombok.Getter;

@Getter
public enum CategoryExperience {

    SCHOOL("Formation"),
    INTERNSHIP("Stage"),
    ALTERNANCE("Alternance"),
    VOLUNTEERING("Bénévolat"),
    CDI("CDI"),
    CDD("CDD"),
    FREELANCE("Freelance"),
    CERTIFICATION("Certification");

    private final String label;

    CategoryExperience(String label) {
        this.label = label;
    }
}