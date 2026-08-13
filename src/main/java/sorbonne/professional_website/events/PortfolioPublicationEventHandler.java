package sorbonne.professional_website.events;

import org.springframework.stereotype.Component;
import sorbonne.professional_website.cache.PortfolioChangePublisher;

@Component
public class PortfolioPublicationEventHandler implements OutboxEventHandler {
    private final PortfolioChangePublisher publisher;
    public PortfolioPublicationEventHandler(PortfolioChangePublisher publisher) { this.publisher = publisher; }
    @Override public boolean supports(String eventType) {
        return "WEBSITE_VERSION_PUBLISHED".equals(eventType) || "WEBSITE_VERSION_ROLLED_BACK".equals(eventType);
    }
    @Override public void handle(OutboxEvent event) { publisher.changed(event.getOwnerId(), "outbox:" + event.getEventType().toLowerCase()); }
}
