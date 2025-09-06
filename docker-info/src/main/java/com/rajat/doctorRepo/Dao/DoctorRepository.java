package com.rajat.doctorRepo.Dao;

import com.rajat.doctorRepo.Entity.Doctor;
import com.rajat.doctorRepo.Entity.DoctorDetailKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface DoctorRepository extends CassandraRepository<Doctor, DoctorDetailKey> {

    @Query("UPDATE doctor_details SET availability_status = :status WHERE id = :id AND email = :email AND name = :name")
    void updateDoctorStatus(UUID id, String email, String name, String status);

    @Query("UPDATE doctor_details SET rating = :rating WHERE id = :id AND email = :email AND name = :name")
    void updateDoctorRating(UUID id, String email, String name, Double rating);


}
