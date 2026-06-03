package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.ProfileDTO;
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

    public void createProfile(ProfileDTO profileDTO) {
        Profile profile = ProfileMapper.fromCreateDTO(profileDTO);
        rpProfile.save(profile);
    }

    @Transactional(readOnly = true)
    public List<ProfileDTO> getAllProfiles() {
        return rpProfile.findAll()
                .stream()
                .map(ProfileMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileDTO getProfileById(Long profileId) {
        Profile profile = findProfileById(profileId);
        return ProfileMapper.toDTO(profile);
    }

    public void updateProfile(Long profileId, ProfileDTO profileDTO) {
        Profile profile = findProfileById(profileId);

        ProfileMapper.updateEntityFromDTO(profile, profileDTO);

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