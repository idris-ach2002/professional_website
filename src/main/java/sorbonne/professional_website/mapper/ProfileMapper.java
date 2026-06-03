package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.ProfileDTO;
import sorbonne.professional_website.entity.Profile;

public final class ProfileMapper {

    private ProfileMapper() {
    }

    public static ProfileDTO toDTO(Profile profile) {
        if (profile == null) {
            return null;
        }

        return new ProfileDTO(
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

    public static Profile fromCreateDTO(ProfileDTO profileDTO) {
        if (profileDTO == null) {
            return null;
        }

        Profile profile = new Profile();

        setPropertiesProfile(profileDTO, profile);

        return profile;
    }

    private static void setPropertiesProfile(ProfileDTO profileDTO, Profile profile) {
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

    public static void updateEntityFromDTO(Profile profile, ProfileDTO profileDTO) {
        if (profile == null || profileDTO == null) {
            return;
        }

        setPropertiesProfile(profileDTO, profile);
    }
}