package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.ContactInfoDTO;
import sorbonne.professional_website.dto.UserDTO;
import sorbonne.professional_website.entity.ContactInfo;
import sorbonne.professional_website.entity.User;

import java.util.ArrayList;
import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        return new UserDTO(
                user.getName(),
                user.getFirstName(),
                user.getAge(),
                user.getAddress(),
                toContactInfoDTOList(user.getContacts()),
                ProfileMapper.toDTO(user.getProf()),
                TimelineMapper.toDTO(user.getTimeline()),
                ProjectMapper.toDTOList(user.getProjects())
        );
    }

    private static List<ContactInfoDTO> toContactInfoDTOList(List<ContactInfo> contacts) {
        if (contacts == null) {
            return List.of();
        }

        List<ContactInfoDTO> contactInfoDTOs = new ArrayList<>();

        for (ContactInfo contact : contacts) {
            contactInfoDTOs.add(ContactInfoMapper.toDTO(contact));
        }

        return contactInfoDTOs;
    }
}