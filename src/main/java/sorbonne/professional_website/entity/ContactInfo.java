package sorbonne.professional_website.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sorbonne.professional_website.entity.enumerations.Contact;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ContactInfo {

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 50)
    private Contact type;

    @Column(name = "contact_value", nullable = false, length = 512)
    private String value;
}