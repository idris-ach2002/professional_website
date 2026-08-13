package sorbonne.professional_website.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public boolean exists(String eventKey) { return repository.findByEventKey(eventKey).isPresent(); }

    public OutboxEvent record(String eventKey, Long ownerId, String aggregateType, Object aggregateId, String eventType, Object payload) {
        return repository.findByEventKey(eventKey).orElseGet(() -> repository.save(OutboxEvent.builder()
                .eventKey(eventKey)
                .ownerId(ownerId)
                .aggregateType(aggregateType)
                .aggregateId(String.valueOf(aggregateId))
                .eventType(eventType)
                .payloadJson(toJson(payload))
                .status(OutboxStatus.PENDING)
                .build()));
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Unable to serialize outbox payload", ex); }
    }
}
