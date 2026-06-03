package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.ContactInfoDTO;
import sorbonne.professional_website.entity.ContactInfo;

public final class ContactInfoMapper {

    private ContactInfoMapper() {
    }

    public static ContactInfoDTO toDTO(ContactInfo contactInfo) {
        if (contactInfo == null) {
            return null;
        }

        return new ContactInfoDTO(
                contactInfo.getType(),
                contactInfo.getValue()
        );
    }
}