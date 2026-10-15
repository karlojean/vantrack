package com.vantrack.trips;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TripFinishedEvent {
    Trip trip;
}
