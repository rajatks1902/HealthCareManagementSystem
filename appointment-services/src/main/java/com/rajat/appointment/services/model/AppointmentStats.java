package com.rajat.appointment.services.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("appointment_stats")
public class AppointmentStats {

    public static final String ALL_DOCTORS = "ALL";

    @PrimaryKey
    private AppointmentStatsKey key;

    @Column("period_end")
    private java.time.LocalDate periodEnd;

    @Column("total_count")
    private long totalCount;

    @Column("pending_count")
    private long pendingCount;

    @Column("confirmed_count")
    private long confirmedCount;

    @Column("rejected_count")
    private long rejectedCount;

    @Column("completed_count")
    private long completedCount;

    @Column("other_count")
    private long otherCount;
}
