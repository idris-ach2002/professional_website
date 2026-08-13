package sorbonne.professional_website.events;

public interface OutboxEventHandler {
    boolean supports(String eventType);
    void handle(OutboxEvent event);
}
