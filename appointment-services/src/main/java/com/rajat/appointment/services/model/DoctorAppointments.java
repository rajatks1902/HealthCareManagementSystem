package com.rajat.appointment.services.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("doctor_appointments")
public class DoctorAppointments {

    @PrimaryKeyColumn(name = "doctor_id", type = PrimaryKeyType.PARTITIONED)
    private UUID doctorId;

    @PrimaryKeyColumn(name = "appointment_id", type = PrimaryKeyType.CLUSTERED)
    private UUID id;
}

