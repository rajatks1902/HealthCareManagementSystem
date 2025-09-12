package com.rajat.appointment.services.Dao;

import com.rajat.appointment.services.model.Appointment;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AppointmentDao extends CassandraRepository<Appointment, UUID>{
}
