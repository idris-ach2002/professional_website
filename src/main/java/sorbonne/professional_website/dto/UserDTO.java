package sorbonne.professional_website.dto;

import java.util.List;

public record UserDTO(
        String name,
        String firstName,
        int age,
        String address,
        List<ContactInfoDTO> contacts,
        ProfileDTO prof,
        TimelineDTO timeline,
        List<ProjectDTO> projects
) {
}