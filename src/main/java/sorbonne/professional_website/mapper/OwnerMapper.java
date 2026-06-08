package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.*;

import java.util.List;

public final class OwnerMapper {

    private OwnerMapper() {
    }

    public static OwnerResponseDTO toResponse(Owner owner) {
        if (owner == null) {
            return null;
        }

        return new OwnerResponseDTO(
                owner.getOwnerId(),
                owner.getName(),
                owner.getFirstName(),
                owner.getAge(),
                owner.getAddress(),
                ContactInfoMapper.toResponseList(owner.getContacts()),
                ProfileMapper.toResponse(owner.getProf()),
                TimelineMapper.toResponse(owner.getTimeline()),
                ProjectMapper.toResponseList(owner.getProjects())
        );
    }

    public static Owner fromRequest(OwnerRequestDTO ownerDTO) {
        if (ownerDTO == null) {
            return null;
        }

        Owner owner = new Owner();
        setOwnerProperties(owner, ownerDTO);
        setOwnerRelations(owner, ownerDTO);

        return owner;
    }

    public static void updateEntityFromRequest(Owner owner, OwnerRequestDTO ownerDTO) {
        if (owner == null || ownerDTO == null) {
            return;
        }

        setOwnerProperties(owner, ownerDTO);
        setOwnerRelations(owner, ownerDTO);
    }

    private static void setOwnerProperties(Owner owner, OwnerRequestDTO ownerDTO) {
        owner.setName(ownerDTO.name());
        owner.setFirstName(ownerDTO.firstName());
        owner.setAge(ownerDTO.age());
        owner.setAddress(ownerDTO.address());
        owner.setActive(ownerDTO.active());
        owner.setContacts(ContactInfoMapper.fromRequestList(ownerDTO.contacts()));
    }

    private static void setOwnerRelations(Owner owner, OwnerRequestDTO ownerDTO) {
        setProfileRelation(owner, ownerDTO);
        setTimelineRelation(owner, ownerDTO);
        setProjectRelations(owner, ownerDTO.projects());
    }

    private static void setProfileRelation(Owner owner, OwnerRequestDTO ownerDTO) {
        Profile profile = ProfileMapper.fromRequest(ownerDTO.prof());

        if (profile != null) {
            profile.setOwner(owner);
        }

        owner.setProf(profile);
    }

    private static void setTimelineRelation(Owner owner, OwnerRequestDTO ownerDTO) {
        Timeline timeline = TimelineMapper.fromRequest(ownerDTO.timeline());

        if (timeline != null) {
            timeline.setOwner(owner);
        }

        owner.setTimeline(timeline);
    }

    private static void setProjectRelations(Owner owner, List<ProjectRequestDTO> projectDTOs) {
        owner.getProjects().clear();

        List<Project> projects = ProjectMapper.fromRequestList(projectDTOs);

        for (Project project : projects) {
            project.setOwner(owner);
            owner.getProjects().add(project);
        }
    }
}
