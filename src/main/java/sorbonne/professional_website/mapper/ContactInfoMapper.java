package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ContactInfoRequestDTO;
import sorbonne.professional_website.dto.response.ContactInfoResponseDTO;
import sorbonne.professional_website.entity.ContactInfo;

import java.util.ArrayList;
import java.util.List;

public final class ContactInfoMapper {

    private ContactInfoMapper() {
    }

    public static ContactInfoResponseDTO toResponse(ContactInfo contactInfo) {
        if (contactInfo == null) {
            return null;
        }

        return new ContactInfoResponseDTO(
                contactInfo.getType(),
                contactInfo.getValue()
        );
    }

    public static ContactInfo fromRequest(ContactInfoRequestDTO contactInfoRequestDTO) {
        if (contactInfoRequestDTO == null) {
            return null;
        }

        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setType(contactInfoRequestDTO.type());
        contactInfo.setValue(contactInfoRequestDTO.value());

        return contactInfo;
    }

    public static List<ContactInfoResponseDTO> toResponseList(List<ContactInfo> contacts) {
        if (contacts == null) {
            return List.of();
        }

        List<ContactInfoResponseDTO> contactInfoResponseDTOs = new ArrayList<>();

        for (ContactInfo contact : contacts) {
            contactInfoResponseDTOs.add(toResponse(contact));
        }

        return contactInfoResponseDTOs;
    }

    public static List<ContactInfo> fromRequestList(List<ContactInfoRequestDTO> contacts) {
        if (contacts == null) {
            return new ArrayList<>();
        }

        List<ContactInfo> contactInfos = new ArrayList<>();

        for (ContactInfoRequestDTO contactDTO : contacts) {
            ContactInfo contactInfo = fromRequest(contactDTO);

            if (contactInfo != null) {
                contactInfos.add(contactInfo);
            }
        }

        return contactInfos;
    }
}
