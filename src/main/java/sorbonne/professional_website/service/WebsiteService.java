package sorbonne.professional_website.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.cache.PublicPortfolioCacheConfig;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.dto.response.ProjectResponseDTO;
import sorbonne.professional_website.dto.response.PublicWebsiteSnapshotResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.ProjectMapper;
import sorbonne.professional_website.repository.OwnerRepository;
import sorbonne.professional_website.translation.service.PortfolioLocalizationService;

import java.text.Normalizer;
import java.time.Instant;
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

    @Cacheable(cacheNames = PublicPortfolioCacheConfig.WEBSITE_LIST_CACHE, key = "#locale == null ? 'fr' : #locale.toLowerCase()", sync = true)
    public List<OwnerResponseDTO> getAllPublicWebsites(String locale) {
        return rpOwner.findAllPublicOwners()
                .stream()
                .map(owner -> localizationService.localizePublic(owner, locale))
                .toList();
    }

    @Cacheable(cacheNames = PublicPortfolioCacheConfig.WEBSITE_CACHE, key = "#ownerId + ':' + (#locale == null ? 'fr' : #locale.toLowerCase())", sync = true)
    public OwnerResponseDTO getPublicWebsiteByOwnerId(Long ownerId, String locale) {
        return localizationService.localizePublic(getPublicOwner(ownerId), locale);
    }

    @Cacheable(cacheNames = PublicPortfolioCacheConfig.WEBSITE_CACHE, key = "'default:' + (#locale == null ? 'fr' : #locale.toLowerCase())", sync = true)
    public OwnerResponseDTO getFirstOwner(String locale) {
        return localizationService.localizePublic(getDefaultPublicOwner(), locale);
    }

    @Cacheable(cacheNames = PublicPortfolioCacheConfig.SEO_CACHE, key = "'default'", sync = true)
    public PublicWebsiteSnapshotResponseDTO getPublicSeoSnapshot() {
        Owner owner = getDefaultPublicOwner();
        return new PublicWebsiteSnapshotResponseDTO(
                Instant.now().toString(),
                localizationService.localizePublic(owner, "fr"),
                localizationService.localizePublic(owner, "en")
        );
    }

    @Cacheable(cacheNames = PublicPortfolioCacheConfig.PROJECT_CACHE, key = "'default:' + #slug + ':' + (#locale == null ? 'fr' : #locale.toLowerCase())", sync = true)
    public ProjectResponseDTO getDefaultProjectBySlug(String slug, String locale) {
        Owner owner = getDefaultPublicOwner();
        return localizeProject(findProject(owner, slug), locale);
    }

    @Cacheable(cacheNames = PublicPortfolioCacheConfig.PROJECT_CACHE, key = "#ownerId + ':' + #slug + ':' + (#locale == null ? 'fr' : #locale.toLowerCase())", sync = true)
    public ProjectResponseDTO getProjectBySlug(Long ownerId, String slug, String locale) {
        return localizeProject(findProject(getPublicOwner(ownerId), slug), locale);
    }

    private Owner getDefaultPublicOwner() {
        return rpOwner.findAllPublicOwners().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));
    }

    private Owner getPublicOwner(Long ownerId) {
        return rpOwner.findPublicOwnerById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Website"));
    }

    private Project findProject(Owner owner, String slug) {
        WebsiteVersion publicVersion = owner.getActivePublishedWebsiteVersion()
                .orElseThrow(() -> new ResourceNotFoundException("Active WebsiteVersion"));
        return publicVersion.getProjects()
                .stream()
                .filter(project -> Boolean.TRUE.equals(project.getPublished()))
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
