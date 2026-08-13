package sorbonne.professional_website.events;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxDispatcherTest {
    @Test void successfulHandlerMarksEventDispatched(){
        OutboxDispatchService dispatchService=mock(OutboxDispatchService.class);
        OutboxEventHandler handler=mock(OutboxEventHandler.class);
        OutboxEvent event=OutboxEvent.builder().id("e1").eventKey("k").ownerId(1L).aggregateType("WebsiteVersion").aggregateId("7").eventType("WEBSITE_VERSION_PUBLISHED").payloadJson("{}").status(OutboxStatus.PROCESSING).build();
        when(dispatchService.claimDue(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(handler.supports("WEBSITE_VERSION_PUBLISHED")).thenReturn(true);

        new OutboxDispatcher(dispatchService,List.of(handler)).dispatchPending();

        verify(handler).handle(event);
        verify(dispatchService).markDispatched("e1");
        verify(dispatchService,never()).markFailed(anyString(),any());
    }

    @Test void failingHandlerDelegatesRetryDecisionToDispatchService(){
        OutboxDispatchService dispatchService=mock(OutboxDispatchService.class);
        OutboxEventHandler handler=mock(OutboxEventHandler.class);
        OutboxEvent event=OutboxEvent.builder().id("e1").eventKey("k").ownerId(1L).aggregateType("WebsiteVersion").aggregateId("7").eventType("WEBSITE_VERSION_PUBLISHED").payloadJson("{}").status(OutboxStatus.PROCESSING).build();
        when(dispatchService.claimDue(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(handler.supports(anyString())).thenReturn(true);
        doThrow(new IllegalStateException("consumer unavailable")).when(handler).handle(event);

        new OutboxDispatcher(dispatchService,List.of(handler)).dispatchPending();

        verify(dispatchService).markFailed(eq("e1"), argThat(error -> error.getMessage().contains("consumer unavailable")));
        verify(dispatchService,never()).markDispatched(anyString());
    }
}
