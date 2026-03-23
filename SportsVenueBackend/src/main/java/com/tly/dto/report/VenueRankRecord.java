package com.tly.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueRankRecord {
    private Long venueId;
    private String venueName;
    private long bookingTotal;
    private long verifiedTotal;
    private long canceledTotal;
    private long violationTotal;
}

