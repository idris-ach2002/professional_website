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
}