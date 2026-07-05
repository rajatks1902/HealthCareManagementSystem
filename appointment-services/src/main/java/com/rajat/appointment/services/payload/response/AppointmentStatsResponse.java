package com.rajat.appointment.services.payload.response;

import java.time.LocalDate;

public record AppointmentStatsResponse(
        String periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        String doctorId,
        long total,
        long pending,
        long confirmed,
        long rejected,
        long completed,
        long other
) {
}
