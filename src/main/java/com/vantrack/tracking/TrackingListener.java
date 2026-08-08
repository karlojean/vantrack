package com.vantrack.tracking;

import com.vantrack.tracking.web.TrackingNotifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;



@Component
public class TrackingListener {

    private final TrackingNotifier trackingNotifier;

    public TrackingListener(TrackingNotifier trackingNotifier) {
        this.trackingNotifier = trackingNotifier;
    }

    @EventListener
    public void handleUserRegistration(SendLocationEvent event) {
        System.out.println(event.getTripLocation().getId() + "AA");
        trackingNotifier.notifyLocation(event.getTripLocation());
    }

}
