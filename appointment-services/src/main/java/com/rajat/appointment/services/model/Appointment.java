package com.rajat.appointment.services.model;

import lombok.*;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an appointment entity with details about doctor, patient, and status.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table("appointment_detail")
public class Appointment {

    @PrimaryKey
    private UUID id;

    @Column("patient_id")
    private UUID patientId;

    @Column("doctor_id")
    private UUID doctorId;

    @Column("appointment_time")
    private LocalDateTime appointmentTime;

    @Column("status")
    private String status; // Enum stored as String (PENDING, CONFIRMED, etc.)

    @Column("notes")
    private String notes;

    @Column("doctor_comments")
    private String doctorComments;
}
