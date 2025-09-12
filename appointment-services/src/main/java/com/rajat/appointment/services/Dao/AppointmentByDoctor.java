package com.rajat.appointment.services.Dao;

import com.rajat.appointment.services.model.Doctor;
import com.rajat.appointment.services.model.DoctorAppointments;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AppointmentByDoctor extends CassandraRepository<DoctorAppointments, UUID> {
}
