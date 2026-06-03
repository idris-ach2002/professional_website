package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.response.ProfileResponseDTO;
import sorbonne.professional_website.entity.Profile;

public final class ProfileMapper {

    private ProfileMapper() {
    }

    public static ProfileResponseDTO toResponse(Profile profile) {
        if (profile == null) {
            return null;
        }

        return new ProfileResponseDTO(
                profile.getId(),
                profile.getTitle(),
                profile.getSubtitle(),
                profile.getHeadline(),
                profile.getShortDescription(),
                profile.getDescription(),
                profile.getLocation(),
                profile.getAvailability(),
                profile.getProfileImageUrl(),
                profile.getLogoUrl(),
                profile.getCvUrl(),
                profile.getPortfolioUrl(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public static Profile fromRequest(ProfileRequestDTO profileDTO) {
        if (profileDTO == null) {
            return null;
        }

        Profile profile = new Profile();
        setPropertiesProfile(profile, profileDTO);

        return profile;
    }

    public static void updateEntityFromRequest(Profile profile, ProfileRequestDTO profileDTO) {
        if (profile == null || profileDTO == null) {
            return;
        }

        setPropertiesProfile(profile, profileDTO);
    }

    private static void setPropertiesProfile(Profile profile, ProfileRequestDTO profileDTO) {
        profile.setTitle(profileDTO.title());
        profile.setSubtitle(profileDTO.subtitle());
        profile.setHeadline(profileDTO.headline());
        profile.setShortDescription(profileDTO.shortDescription());
        profile.setDescription(profileDTO.description());
        profile.setLocation(profileDTO.location());
        profile.setAvailability(profileDTO.availability());
        profile.setProfileImageUrl(profileDTO.profileImageUrl());
        profile.setLogoUrl(profileDTO.logoUrl());
        profile.setCvUrl(profileDTO.cvUrl());
        profile.setPortfolioUrl(profileDTO.portfolioUrl());
    }
}
