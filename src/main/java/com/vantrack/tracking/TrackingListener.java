package com.vantrack.tracking;

import com.vantrack.tracking.web.websocket.TrackingNotifier;
import com.vantrack.trips.TripFinishedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
public class TrackingListener {

    private final TrackingNotifier trackingNotifier;

    public TrackingListener(TrackingNotifier trackingNotifier) {
        this.trackingNotifier = trackingNotifier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLocationSent(SendLocationEvent event) {
        trackingNotifier.notifyLocation(event.tripLocation());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFinishTrip(TripFinishedEvent event) {
        trackingNotifier.notifyFinishTrip(event.getTrip());
    }
}
