package com.vantrack.tracking.web.websocket;

import com.vantrack.tracking.TripLocation;
import com.vantrack.tracking.web.dto.TripLocationResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TrackingNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    public TrackingNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyLocation(TripLocation location) {
        messagingTemplate.convertAndSend(
                "/topic/trips/" + location.getTrip().getId(),
                TripLocationResponse.fromEntity(location)
        );
    }
}
