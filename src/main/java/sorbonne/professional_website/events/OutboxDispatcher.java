package sorbonne.professional_website.events;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxDispatcher {
    private final OutboxDispatchService dispatchService;
    private final List<OutboxEventHandler> handlers;

    public OutboxDispatcher(OutboxDispatchService dispatchService, List<OutboxEventHandler> handlers) {
        this.dispatchService = dispatchService;
        this.handlers = handlers;
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-delay-ms:500}")
    public void dispatchPending() {
        dispatchService.recoverStale(sorbonne.professional_website.time.PlatformTime.utcNow().minusMinutes(2));
        for (OutboxEvent event : dispatchService.claimDue(sorbonne.professional_website.time.PlatformTime.utcNow())) {
            try {
                for (OutboxEventHandler handler : handlers) {
                    if (handler.supports(event.getEventType())) handler.handle(event);
                }
                dispatchService.markDispatched(event.getId());
            } catch (RuntimeException exception) {
                dispatchService.markFailed(event.getId(), exception);
            }
        }
    }
}
