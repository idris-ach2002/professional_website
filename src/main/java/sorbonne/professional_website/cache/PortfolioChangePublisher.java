package sorbonne.professional_website.cache;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PortfolioChangePublisher {

    private final ApplicationEventPublisher publisher;

    public PortfolioChangePublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void changed(Long ownerId, String reason) {
        publisher.publishEvent(new PortfolioChangedEvent(ownerId, reason));
    }
}
