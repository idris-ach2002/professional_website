package sorbonne.professional_website.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.OwnerMapper;
import sorbonne.professional_website.repository.OwnerRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WebsiteService {

    private final OwnerRepository rpOwner;

    public WebsiteService(OwnerRepository rpOwner) {
        this.rpOwner = rpOwner;
    }

    public List<OwnerResponseDTO> getAllPublicWebsites() {
        return rpOwner.findAll()
                .stream()
                .filter(owner -> Boolean.TRUE.equals(owner.getActive()))
                .filter(owner -> owner.getActiveWebsiteVersion().isPresent())
                .map(OwnerMapper::toResponse)
                .toList();
    }

    public OwnerResponseDTO getPublicWebsiteByOwnerId(Long ownerId) {
        Owner owner = rpOwner.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));

        if (!Boolean.TRUE.equals(owner.getActive())) {
            throw new ResourceNotFoundException("Website");
        }

        if (owner.getActiveWebsiteVersion().isEmpty()) {
            throw new ResourceNotFoundException("Active WebsiteVersion");
        }

        return OwnerMapper.toResponse(owner);
    }

    public OwnerResponseDTO getFirstOwner() {
        Owner owner = rpOwner.findFirstByOrderByOwnerIdAsc()
                .orElseThrow(() -> new EntityNotFoundException("No owner found"));

        return OwnerMapper.toResponse(owner);
    }

}
