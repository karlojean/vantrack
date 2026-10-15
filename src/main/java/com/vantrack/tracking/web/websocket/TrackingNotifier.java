package com.vantrack.tracking.web.websocket;

import com.vantrack.tracking.TripLocation;
import com.vantrack.tracking.web.dto.FinishTripResponse;
import com.vantrack.tracking.web.dto.TripLocationResponse;
import com.vantrack.trips.Trip;
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

    public void notifyFinishTrip(Trip trip) {
        messagingTemplate.convertAndSend(
                "/topic/trips/" + trip.getId(),
                FinishTripResponse.fromEntity(trip)
        );
    }
}
