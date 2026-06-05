package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.*;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ProfileMapper;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.mapper.TimelineMapper;
import sorbonne.professional_website.mapper.OwnerMapper;
import sorbonne.professional_website.repository.OwnerRepository;

import java.util.List;

@Service
@Transactional
public class OwnerService {

    private final OwnerRepository rpOwner;

    public OwnerService(OwnerRepository rpOwner) {
        this.rpOwner = rpOwner;
    }

    public void createOwner(OwnerRequestDTO ownerRequestDTO) {
        Owner owner = OwnerMapper.fromRequest(ownerRequestDTO);
        rpOwner.save(owner);
    }

    @Transactional(readOnly = true)
    public List<OwnerResponseDTO> getAllOwners() {
        return rpOwner.findAll()
                .stream()
                .map(OwnerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerResponseDTO getOwnerById(Long ownerId) {
        Owner owner = findOwnerById(ownerId);
        return OwnerMapper.toResponse(owner);
    }

    public void updateOwner(Long ownerId, OwnerRequestDTO ownerRequestDTO) {
        Owner owner = findOwnerById(ownerId);
        OwnerMapper.updateEntityFromRequest(owner, ownerRequestDTO);
        rpOwner.save(owner);
    }

    public void deleteOwner(Long ownerId) {
        Owner owner = findOwnerById(ownerId);
        rpOwner.delete(owner);
    }

    public void createOrReplaceProfile(Long ownerId, ProfileRequestDTO profileRequestDTO) {
        Owner owner = findOwnerById(ownerId);

        Profile profile = ProfileMapper.fromRequest(profileRequestDTO);
        profile.setOwner(owner);
        owner.setProf(profile);

        rpOwner.save(owner);
    }

    public void createOrReplaceTimeline(Long ownerId, TimelineRequestDTO timelineRequestDTO) {
        Owner owner = findOwnerById(ownerId);

        Timeline timeline = TimelineMapper.fromRequest(timelineRequestDTO);
        timeline.setOwner(owner);
        owner.setTimeline(timeline);

        rpOwner.save(owner);
    }

    public void addProjectToOwner(Long ownerId, ProjectRequestDTO projectRequestDTO) {
        Owner owner = findOwnerById(ownerId);

        Project project = ProjectMapper.fromRequest(projectRequestDTO);
        project.setOwner(owner);
        owner.getProjects().add(project);

        rpOwner.save(owner);
    }

    private Owner findOwnerById(Long ownerId) {
        return rpOwner.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));
    }
}
