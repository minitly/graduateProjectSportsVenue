package com.tly.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatResponse {
    private long bookingTotal;
    private long bookingVerifiedTotal;
    private long bookingCanceledTotal;
    private long bookingViolationTotal;
}

