package com.rajat.patient.info.Dao;

import com.rajat.patient.info.Entity.PatientInfo;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientDao extends CassandraRepository<PatientInfo, UUID> {
}
