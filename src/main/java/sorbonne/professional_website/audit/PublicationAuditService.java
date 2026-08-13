package sorbonne.professional_website.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PublicationAuditService {
    private final PublicationAuditRepository repository;
    private final ObjectMapper objectMapper;

    public PublicationAuditService(PublicationAuditRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PublicationAuditEntry record(
            Long ownerId,
            Long versionId,
            String action,
            String correlationId,
            Object before,
            Object after,
            Object metadata
    ) {
        PublicationAuditEntry entry = PublicationAuditEntry.builder()
                .ownerId(ownerId)
                .versionId(versionId)
                .action(action)
                .actor(currentActor())
                .correlationId(correlationId)
                .beforeJson(toJson(before))
                .afterJson(toJson(after))
                .metadataJson(toJson(metadata == null ? Map.of() : metadata))
                .build();
        return repository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<PublicationAuditResponse> list(Long ownerId, Long versionId) {
        var entries = versionId == null
                ? repository.findTop200ByOwnerIdOrderByCreatedAtDesc(ownerId)
                : repository.findTop200ByOwnerIdAndVersionIdOrderByCreatedAtDesc(ownerId, versionId);
        return entries.stream().map(PublicationAuditResponse::from).toList();
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return "SYSTEM";
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) return "SYSTEM";
        return name;
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize publication audit payload", exception);
        }
    }
}
