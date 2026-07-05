package com.rajat.appointment.services.payload.event;

import com.rajat.appointment.services.model.Doctor;
import com.rajat.appointment.services.model.Patient;

import java.time.LocalDateTime;

/**
 * Kafka contract used by notification-services.
 * Keep this separate from the Cassandra entity so database schema changes do not break consumers.
 */
public record AppointmentNotificationEvent(
        String id,
        Patient patient,
        Doctor doctor,
        LocalDateTime appointmentTime,
        String status,
        String notes,
        String doctorComments
) {
}
