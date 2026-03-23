package com.tly.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingTrendRecord {
    private LocalDate date;
    private long bookingTotal;
    private long verifiedTotal;
    private long canceledTotal;
    private long violationTotal;
}

