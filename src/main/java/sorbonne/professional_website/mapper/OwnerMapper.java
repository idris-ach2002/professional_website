package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;

import java.util.List;

public final class OwnerMapper {

    private OwnerMapper() {
    }

    public static OwnerResponseDTO toResponse(Owner owner) {
        if (owner == null) {
            return null;
        }

        WebsiteVersion activeVersion = owner.getActiveWebsiteVersion().orElse(null);

        return new OwnerResponseDTO(
                owner.getOwnerId(),
                owner.getName(),
                owner.getFirstName(),
                owner.getAge(),
                owner.getActive(),
                owner.getAddress(),
                ContactInfoMapper.toResponseList(owner.getContacts()),
                activeVersion != null ? ProfileMapper.toResponse(activeVersion.getProfile()) : null,
                activeVersion != null ? TimelineMapper.toResponse(activeVersion.getTimeline()) : null,
                activeVersion != null ? ProjectMapper.toResponseList(activeVersion.getProjects()) : List.of(),
                WebsiteVersionMapper.toSummaryResponseList(owner.getWebsiteVersions())
        );
    }

    public static Owner fromRequest(OwnerRequestDTO ownerDTO) {
        if (ownerDTO == null) {
            return null;
        }

        Owner owner = new Owner();
        setOwnerProperties(owner, ownerDTO);
        createInitialWebsiteVersion(owner, ownerDTO);

        return owner;
    }

    public static void updateEntityFromRequest(Owner owner, OwnerRequestDTO ownerDTO) {
        if (owner == null || ownerDTO == null) {
            return;
        }

        setOwnerProperties(owner, ownerDTO);
    }

    private static void setOwnerProperties(Owner owner, OwnerRequestDTO ownerDTO) {
        owner.setName(ownerDTO.name());
        owner.setFirstName(ownerDTO.firstName());
        owner.setAge(ownerDTO.age());
        owner.setAddress(ownerDTO.address());
        owner.setActive(ownerDTO.active());
        owner.setContacts(ContactInfoMapper.fromRequestList(ownerDTO.contacts()));
    }

    private static void createInitialWebsiteVersion(Owner owner, OwnerRequestDTO ownerDTO) {
        if (!hasInitialWebsiteContent(ownerDTO)) {
            return;
        }

        Profile profile = ProfileMapper.fromRequest(ownerDTO.prof());
        Timeline timeline = TimelineMapper.fromRequest(ownerDTO.timeline());
        List<Project> projects = ProjectMapper.fromRequestList(ownerDTO.projects());

        WebsiteVersion initialVersion = WebsiteVersionMapper.createInitialVersionFromOwnerRequest(
                ownerDTO.versionTag(),
                ownerDTO.versionLabel(),
                ownerDTO.versionDescription(),
                ownerDTO.versionPublished(),
                profile,
                timeline,
                projects
        );

        initialVersion.setOwner(owner);
        owner.getWebsiteVersions().add(initialVersion);
    }

    private static boolean hasInitialWebsiteContent(OwnerRequestDTO ownerDTO) {
        List<ProjectRequestDTO> projects = ownerDTO.projects();

        return ownerDTO.prof() != null
                || ownerDTO.timeline() != null
                || (projects != null && !projects.isEmpty());
    }
}
