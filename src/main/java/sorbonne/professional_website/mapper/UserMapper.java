package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.UserRequestDTO;
import sorbonne.professional_website.dto.response.UserResponseDTO;
import sorbonne.professional_website.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponseDTO toResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getFirstName(),
                user.getAge(),
                user.getAddress(),
                ContactInfoMapper.toResponseList(user.getContacts()),
                ProfileMapper.toResponse(user.getProf()),
                TimelineMapper.toResponse(user.getTimeline()),
                ProjectMapper.toResponseList(user.getProjects())
        );
    }

    public static User fromRequest(UserRequestDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        User user = new User();
        setUserProperties(user, userDTO);

        return user;
    }

    public static void updateEntityFromRequest(User user, UserRequestDTO userDTO) {
        if (user == null || userDTO == null) {
            return;
        }

        setUserProperties(user, userDTO);
    }

    private static void setUserProperties(User user, UserRequestDTO userDTO) {
        user.setName(userDTO.name());
        user.setFirstName(userDTO.firstName());
        user.setAge(userDTO.age());
        user.setAddress(userDTO.address());
        user.setContacts(ContactInfoMapper.fromRequestList(userDTO.contacts()));
    }
}
