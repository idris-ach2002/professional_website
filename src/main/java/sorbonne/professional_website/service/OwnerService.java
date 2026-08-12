package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import sorbonne.professional_website.cache.PortfolioChangePublisher;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.OwnerRequestDTO;
import sorbonne.professional_website.dto.response.OwnerResponseDTO;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.exception.PreconditionFailedException;
import sorbonne.professional_website.mapper.OwnerMapper;
import sorbonne.professional_website.repository.OwnerRepository;

import java.util.List;

@Service
@Transactional
public class OwnerService {

    private final OwnerRepository rpOwner;
    private final PortfolioChangePublisher changePublisher;

    public OwnerService(
            OwnerRepository rpOwner,
            PortfolioChangePublisher changePublisher
    ) {
        this.rpOwner = rpOwner;
        this.changePublisher = changePublisher;
    }

    public void createOwner(OwnerRequestDTO ownerRequestDTO) {
        Owner owner = OwnerMapper.fromRequest(ownerRequestDTO);
        Owner saved = rpOwner.save(owner);
        changePublisher.changed(saved.getOwnerId(), "owner-created");
    }

    @Transactional(readOnly = true)
    public List<OwnerResponseDTO> getAllOwners() {
        return rpOwner.findAll()
                .stream()
                .map(OwnerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerResponseDTO getOwnerById(Long ownerId) {
        Owner owner = findOwnerById(ownerId);
        return OwnerMapper.toResponse(owner);
    }

    public OwnerResponseDTO updateOwner(Long ownerId, long expectedRevision, OwnerRequestDTO ownerRequestDTO) {
        Owner owner = lockOwner(ownerId);
        requireOwnerRevision(owner, expectedRevision);
        OwnerMapper.updateEntityFromRequest(owner, ownerRequestDTO);
        Owner saved = rpOwner.saveAndFlush(owner);
        changePublisher.changed(ownerId, "owner-updated");
        return OwnerMapper.toResponse(saved);
    }

    public void deleteOwner(Long ownerId, long expectedRevision) {
        Owner owner = lockOwner(ownerId);
        requireOwnerRevision(owner, expectedRevision);
        rpOwner.delete(owner);
        changePublisher.changed(ownerId, "owner-deleted");
    }

    private Owner lockOwner(Long ownerId) {
        return rpOwner.lockByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));
    }

    private static void requireOwnerRevision(Owner owner, long expectedRevision) {
        if (owner.getRowVersion() != expectedRevision) {
            throw new PreconditionFailedException(
                    "Owner modifié depuis votre dernière lecture (attendu=" + expectedRevision
                            + ", courant=" + owner.getRowVersion() + ")."
            );
        }
    }

    private Owner findOwnerById(Long ownerId) {
        return rpOwner.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner"));
    }
}
