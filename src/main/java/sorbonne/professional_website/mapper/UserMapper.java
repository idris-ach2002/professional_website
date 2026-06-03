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

    public static User fromCreateDTO(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        User user = new User();

        user.setName(userDTO.name());
        user.setFirstName(userDTO.firstName());
        user.setAge(userDTO.age());
        user.setAddress(userDTO.address());
        user.setContacts(toContactInfoEntityList(userDTO.contacts()));

        return user;
    }

    public static void updateEntityFromDTO(User user, UserDTO userDTO) {
        if (user == null || userDTO == null) {
            return;
        }

        user.setName(userDTO.name());
        user.setFirstName(userDTO.firstName());
        user.setAge(userDTO.age());
        user.setAddress(userDTO.address());
        user.setContacts(toContactInfoEntityList(userDTO.contacts()));
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

    private static List<ContactInfo> toContactInfoEntityList(List<ContactInfoDTO> contacts) {
        if (contacts == null) {
            return new ArrayList<>();
        }

        List<ContactInfo> contactInfos = new ArrayList<>();

        for (ContactInfoDTO contactDTO : contacts) {
            if (contactDTO != null) {
                ContactInfo contactInfo = new ContactInfo();
                contactInfo.setType(contactDTO.type());
                contactInfo.setValue(contactDTO.value());

                contactInfos.add(contactInfo);
            }
        }

        return contactInfos;
    }
}