package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.response.ProfileResponseDTO;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ProfileMapper;
import sorbonne.professional_website.repository.ProfileRepository;

import java.util.List;

@Service
@Transactional
public class ProfileService {

    private final ProfileRepository rpProfile;

    public ProfileService(ProfileRepository rpProfile) {
        this.rpProfile = rpProfile;
    }

    public void createProfile(ProfileRequestDTO profileRequestDTO) {
        Profile profile = ProfileMapper.fromRequest(profileRequestDTO);
        rpProfile.save(profile);
    }

    @Transactional(readOnly = true)
    public List<ProfileResponseDTO> getAllProfiles() {
        return rpProfile.findAll()
                .stream()
                .map(ProfileMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponseDTO getProfileById(Long profileId) {
        Profile profile = findProfileById(profileId);
        return ProfileMapper.toResponse(profile);
    }

    public void updateProfile(Long profileId, ProfileRequestDTO profileRequestDTO) {
        Profile profile = findProfileById(profileId);
        ProfileMapper.updateEntityFromRequest(profile, profileRequestDTO);
        rpProfile.save(profile);
    }

    public void deleteProfile(Long profileId) {
        Profile profile = findProfileById(profileId);
        rpProfile.delete(profile);
    }

    private Profile findProfileById(Long profileId) {
        return rpProfile.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile"));
    }
}
