package sorbonne.professional_website.events;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxDispatchService {
    private static final int BATCH_LIMIT = 100;
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository repository;

    public OutboxDispatchService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<OutboxEvent> claimDue(LocalDateTime now) {
        List<OutboxEvent> due = repository.lockDue(OutboxStatus.PENDING, now);
        if (due.size() > BATCH_LIMIT) due = due.subList(0, BATCH_LIMIT);
        for (OutboxEvent event : due) {
            event.setStatus(OutboxStatus.PROCESSING);
            event.setClaimedAt(now);
            event.setAttempts(event.getAttempts() + 1);
        }
        return List.copyOf(due);
    }

    @Transactional
    public void markDispatched(String eventId) {
        OutboxEvent event = lock(eventId);
        if (event.getStatus() != OutboxStatus.PROCESSING) return;
        event.setStatus(OutboxStatus.DISPATCHED);
        event.setDispatchedAt(sorbonne.professional_website.time.PlatformTime.utcNow());
        event.setClaimedAt(null);
        event.setLastError(null);
    }

    @Transactional
    public void markFailed(String eventId, Throwable failure) {
        OutboxEvent event = lock(eventId);
        if (event.getStatus() != OutboxStatus.PROCESSING) return;
        event.setLastError(failure == null ? "Unknown outbox dispatch failure" : safeMessage(failure));
        event.setClaimedAt(null);
        if (event.getAttempts() >= MAX_ATTEMPTS) {
            event.setStatus(OutboxStatus.DEAD);
            event.setNextAttemptAt(sorbonne.professional_website.time.PlatformTime.utcNow());
        } else {
            event.setStatus(OutboxStatus.PENDING);
            long backoffSeconds = Math.min(60, 1L << Math.min(event.getAttempts(), 5));
            event.setNextAttemptAt(sorbonne.professional_website.time.PlatformTime.utcNow().plusSeconds(backoffSeconds));
        }
    }

    @Transactional
    public int recoverStale(LocalDateTime cutoff) {
        List<OutboxEvent> stale = repository.lockStaleProcessing(OutboxStatus.PROCESSING, cutoff);
        LocalDateTime now = sorbonne.professional_website.time.PlatformTime.utcNow();
        for (OutboxEvent event : stale) {
            event.setStatus(OutboxStatus.PENDING);
            event.setClaimedAt(null);
            event.setNextAttemptAt(now);
            event.setLastError("Recovered after interrupted dispatcher execution.");
        }
        return stale.size();
    }

    @Transactional
    public OutboxEventResponse retryDead(Long ownerId, String eventId) {
        OutboxEvent event = repository.lockById(eventId)
                .filter(candidate -> candidate.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("OutboxEvent"));
        if (event.getStatus() != OutboxStatus.DEAD) {
            throw new IllegalStateException("Only DEAD outbox events can be retried manually.");
        }
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setClaimedAt(null);
        event.setDispatchedAt(null);
        event.setNextAttemptAt(sorbonne.professional_website.time.PlatformTime.utcNow());
        event.setLastError(null);
        return OutboxEventResponse.from(event);
    }

    private OutboxEvent lock(String eventId) {
        return repository.lockById(eventId).orElseThrow(() -> new ResourceNotFoundException("OutboxEvent"));
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) message = failure.getClass().getSimpleName();
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
