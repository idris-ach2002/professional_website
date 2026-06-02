package sorbonne.professional_website.entity.enumerations;

import lombok.Getter;

@Getter
public enum Contact {
    EMAIL("Email"),
    PHONE_NUMBER("Téléphone"),
    LINKEDIN("LinkedIn"),
    GITHUB("GitHub"),
    PORTFOLIO("Portfolio"),
    WEBSITE("Site web"),
    TWITTER("Twitter / X"),
    FACEBOOK("Facebook"),
    INSTAGRAM("Instagram"),
    WHATSAPP("WhatsApp");

    private final String label;

    Contact(String label) {
        this.label = label;
    }
}