package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.UserRequestDTO;
import sorbonne.professional_website.dto.response.UserResponseDTO;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.User;

import java.util.List;

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
        setUserRelations(user, userDTO);

        return user;
    }

    public static void updateEntityFromRequest(User user, UserRequestDTO userDTO) {
        if (user == null || userDTO == null) {
            return;
        }

        setUserProperties(user, userDTO);
        setUserRelations(user, userDTO);
    }

    private static void setUserProperties(User user, UserRequestDTO userDTO) {
        user.setName(userDTO.name());
        user.setFirstName(userDTO.firstName());
        user.setAge(userDTO.age());
        user.setAddress(userDTO.address());
        user.setContacts(ContactInfoMapper.fromRequestList(userDTO.contacts()));
    }

    private static void setUserRelations(User user, UserRequestDTO userDTO) {
        setProfileRelation(user, userDTO);
        setTimelineRelation(user, userDTO);
        setProjectRelations(user, userDTO.projects());
    }

    private static void setProfileRelation(User user, UserRequestDTO userDTO) {
        Profile profile = ProfileMapper.fromRequest(userDTO.prof());

        if (profile != null) {
            profile.setUser(user);
        }

        user.setProf(profile);
    }

    private static void setTimelineRelation(User user, UserRequestDTO userDTO) {
        Timeline timeline = TimelineMapper.fromRequest(userDTO.timeline());

        if (timeline != null) {
            timeline.setUser(user);
        }

        user.setTimeline(timeline);
    }

    private static void setProjectRelations(User user, List<ProjectRequestDTO> projectDTOs) {
        user.getProjects().clear();

        List<Project> projects = ProjectMapper.fromRequestList(projectDTOs);

        for (Project project : projects) {
            project.setUser(user);
            user.getProjects().add(project);
        }
    }
}
