package com.vantrack.tracking;

import com.vantrack.tracking.web.websocket.TrackingNotifier;
import org.springframework.context.event.EventListener;
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
    public void handleUserRegistration(SendLocationEvent event) {
        trackingNotifier.notifyLocation(event.getTripLocation());
    }

}
