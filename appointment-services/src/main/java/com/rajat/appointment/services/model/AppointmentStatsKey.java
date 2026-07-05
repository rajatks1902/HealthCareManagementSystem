package com.rajat.appointment.services.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyClass
public class AppointmentStatsKey implements Serializable {

    @PrimaryKeyColumn(name = "period_type", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private String periodType;

    @PrimaryKeyColumn(name = "period_start", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private LocalDate periodStart;

    @PrimaryKeyColumn(name = "doctor_id", type = PrimaryKeyType.CLUSTERED, ordinal = 2)
    private String doctorId;
}
