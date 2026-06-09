package sorbonne.professional_website.dto.response;

import java.util.List;

public record OwnerResponseDTO(
        Long ownerId,
        String name,
        String firstName,
        int age,
        Boolean active,
        String address,
        List<ContactInfoResponseDTO> contacts,
        ProfileResponseDTO prof,
        TimelineResponseDTO timeline,
        List<ProjectResponseDTO> projects,
        List<WebsiteVersionSummaryResponseDTO> websiteVersions
) {
}
