package com.vantrack.tracking;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendLocationEvent {
    public TripLocation tripLocation;
}
