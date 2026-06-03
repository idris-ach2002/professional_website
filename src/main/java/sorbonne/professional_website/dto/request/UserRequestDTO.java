package sorbonne.professional_website.dto.request;

import java.util.List;

public record UserRequestDTO(
        String name,
        String firstName,
        int age,
        String address,
        List<ContactInfoRequestDTO> contacts,
        ProfileRequestDTO prof,
        TimelineRequestDTO timeline,
        List<ProjectRequestDTO> projects
) {
}
