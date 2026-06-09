package sorbonne.professional_website.mapper;

import sorbonne.professional_website.dto.request.WebsiteVersionRequestDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionResponseDTO;
import sorbonne.professional_website.dto.response.WebsiteVersionSummaryResponseDTO;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;

import java.util.ArrayList;
import java.util.List;

public final class WebsiteVersionMapper {

    private WebsiteVersionMapper() {
    }

    public static WebsiteVersionResponseDTO toResponse(WebsiteVersion version) {
        if (version == null) {
            return null;
        }

        return new WebsiteVersionResponseDTO(
                version.getId(),
                version.getVersionTag(),
                version.getLabel(),
                version.getDescription(),
                version.getActive(),
                version.getPublished(),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                ProfileMapper.toResponse(version.getProfile()),
                TimelineMapper.toResponse(version.getTimeline()),
                ProjectMapper.toResponseList(version.getProjects())
        );
    }

    public static WebsiteVersionSummaryResponseDTO toSummaryResponse(WebsiteVersion version) {
        if (version == null) {
            return null;
        }

        return new WebsiteVersionSummaryResponseDTO(
                version.getId(),
                version.getVersionTag(),
                version.getLabel(),
                version.getDescription(),
                version.getActive(),
                version.getPublished(),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }

    public static List<WebsiteVersionSummaryResponseDTO> toSummaryResponseList(List<WebsiteVersion> versions) {
        if (versions == null) {
            return List.of();
        }

        return versions.stream()
                .map(WebsiteVersionMapper::toSummaryResponse)
                .toList();
    }

    public static List<WebsiteVersionResponseDTO> toResponseList(List<WebsiteVersion> versions) {
        if (versions == null) {
            return List.of();
        }

        return versions.stream()
                .map(WebsiteVersionMapper::toResponse)
                .toList();
    }

    public static WebsiteVersion fromRequest(WebsiteVersionRequestDTO versionDTO) {
        WebsiteVersion version = new WebsiteVersion();
        updateEntityFromRequest(version, versionDTO);
        return version;
    }

    public static void updateEntityFromRequest(WebsiteVersion version, WebsiteVersionRequestDTO versionDTO) {
        if (version == null || versionDTO == null) {
            return;
        }

        if (versionDTO.versionTag() != null) {
            version.setVersionTag(versionDTO.versionTag());
        }

        if (versionDTO.label() != null) {
            version.setLabel(versionDTO.label());
        }

        version.setDescription(versionDTO.description());

        if (versionDTO.published() != null) {
            version.setPublished(versionDTO.published());
        }

        if (versionDTO.prof() != null) {
            Profile profile = ProfileMapper.fromRequest(versionDTO.prof());
            version.attachProfile(profile);
        }

        if (versionDTO.timeline() != null) {
            Timeline timeline = TimelineMapper.fromRequest(versionDTO.timeline());
            version.attachTimeline(timeline);
        }

        if (versionDTO.projects() != null) {
            List<Project> projects = ProjectMapper.fromRequestList(versionDTO.projects());
            version.clearAndAttachProjects(projects);
        }
    }

    public static WebsiteVersion createInitialVersionFromOwnerRequest(
            String versionTag,
            String versionLabel,
            String versionDescription,
            Boolean versionPublished,
            Profile profile,
            Timeline timeline,
            List<Project> projects
    ) {
        WebsiteVersion version = WebsiteVersion.builder()
                .versionTag(defaultIfBlank(versionTag, "v1"))
                .label(defaultIfBlank(versionLabel, "Version initiale"))
                .description(versionDescription)
                .active(true)
                .published(versionPublished != null ? versionPublished : true)
                .build();

        version.attachProfile(profile);
        version.attachTimeline(timeline);
        version.clearAndAttachProjects(projects != null ? projects : new ArrayList<>());

        return version;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}
