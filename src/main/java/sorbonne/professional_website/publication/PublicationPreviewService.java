package sorbonne.professional_website.publication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.*;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.repository.WebsiteVersionRepository;
import sorbonne.professional_website.translation.service.PortfolioLocalizationService;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PublicationPreviewService {
    private final OwnerRepository ownerRepository;
    private final WebsiteVersionRepository versionRepository;
    private final PortfolioLocalizationService localizationService;

    public PublicationPreviewService(
            OwnerRepository ownerRepository,
            WebsiteVersionRepository versionRepository,
            PortfolioLocalizationService localizationService
    ) {
        this.ownerRepository = ownerRepository;
        this.versionRepository = versionRepository;
        this.localizationService = localizationService;
    }

    public OwnerResponseDTO preview(Long ownerId, Long versionId, String locale) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));
        WebsiteVersion version = versionRepository.findByIdAndOwnerOwnerId(versionId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Website version not found: " + versionId));

        List<Project> publicProjects = version.getProjects() == null
                ? List.of()
                : version.getProjects().stream()
                        .filter(project -> project.getPublished() == null || Boolean.TRUE.equals(project.getPublished()))
                        .toList();

        OwnerResponseDTO source = new OwnerResponseDTO(
                owner.getOwnerId(),
                owner.getRowVersion(),
                owner.getName(),
                owner.getFirstName(),
                owner.getAge(),
                owner.getActive(),
                owner.getAddress(),
                ContactInfoMapper.toResponseList(owner.getContacts()),
                ProfileMapper.toResponse(version.getProfile()),
                TimelineMapper.toResponse(version.getTimeline()),
                ProjectMapper.toResponseList(publicProjects),
                List.of(WebsiteVersionMapper.toSummaryResponse(version)),
                "fr",
                List.of()
        );

        return localizationService.localizeSnapshot(source, locale);
    }
}
