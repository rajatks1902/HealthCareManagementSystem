package com.rajat.doctorRepo.Dao;

import com.rajat.doctorRepo.Entity.DoctorBasicInfo;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DoctorBasicInfoRepository extends CassandraRepository<DoctorBasicInfo, UUID> {
}
