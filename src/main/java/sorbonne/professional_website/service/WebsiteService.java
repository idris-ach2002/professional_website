package sorbonne.professional_website.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.translation.service.PortfolioLocalizationService;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class WebsiteService {

    private final OwnerRepository rpOwner;
    private final PortfolioLocalizationService localizationService;

    public WebsiteService(OwnerRepository rpOwner, PortfolioLocalizationService localizationService) {
        this.rpOwner = rpOwner;
        this.localizationService = localizationService;
    }

    public List<OwnerResponseDTO> getAllPublicWebsites(String locale) {
        return rpOwner.findAll()
                .stream()
                .filter(owner -> Boolean.TRUE.equals(owner.getActive()))
                .filter(owner -> owner.getActiveWebsiteVersion().isPresent())
                .map(owner -> localizationService.localize(owner, locale))
                .toList();
    }

    public OwnerResponseDTO getPublicWebsiteByOwnerId(Long ownerId, String locale) {
        Owner owner = getPublicOwner(ownerId);
        return localizationService.localize(owner, locale);
    }

    public OwnerResponseDTO getFirstOwner(String locale) {
        Owner owner = rpOwner.findFirstByOrderByOwnerIdAsc()
                .orElseThrow(() -> new EntityNotFoundException("No owner found"));
        return localizationService.localize(owner, locale);
    }

    public ProjectResponseDTO getDefaultProjectBySlug(String slug, String locale) {
        Owner owner = rpOwner.findFirstByOrderByOwnerIdAsc()
                .orElseThrow(() -> new EntityNotFoundException("No owner found"));
        return localizeProject(findProject(owner, slug), locale);
    }

    public ProjectResponseDTO getProjectBySlug(Long ownerId, String slug, String locale) {
        Owner owner = getPublicOwner(ownerId);
        return localizeProject(findProject(owner, slug), locale);
    }

    private Owner getPublicOwner(Long ownerId) {
        Owner owner = rpOwner.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));
        if (!Boolean.TRUE.equals(owner.getActive())) {
            throw new ResourceNotFoundException("Website");
        }
        if (owner.getActiveWebsiteVersion().isEmpty()) {
            throw new ResourceNotFoundException("Active WebsiteVersion");
        }
        return owner;
    }

    private Project findProject(Owner owner, String slug) {
        return owner.getActiveWebsiteVersion()
                .orElseThrow(() -> new ResourceNotFoundException("Active WebsiteVersion"))
                .getProjects()
                .stream()
                .filter(project -> !Boolean.FALSE.equals(project.getPublished()))
                .filter(project -> slugify(project.getTitle()).equals(slugify(slug)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Project"));
    }

    private ProjectResponseDTO localizeProject(Project project, String locale) {
        return localizationService.localizeProject(ProjectMapper.toResponse(project), locale);
    }

    private static String slugify(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
