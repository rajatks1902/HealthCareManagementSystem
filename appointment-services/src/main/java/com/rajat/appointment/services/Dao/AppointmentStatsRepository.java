package com.rajat.appointment.services.Dao;

import com.rajat.appointment.services.model.AppointmentStats;
import com.rajat.appointment.services.model.AppointmentStatsKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentStatsRepository extends CassandraRepository<AppointmentStats, AppointmentStatsKey> {
}
