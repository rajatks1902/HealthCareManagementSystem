package com.rajat.doctorRepo.Entity;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Table("doctor_details")
public class Doctor {

    @PrimaryKey
    private  DoctorDetailKey doctorDetailKey;

    @Column("speciality")
    private String speciality;

    @Column("year_of_exp")
    private Double yearOfExp;

    @Column("rating")
    private Double Rating =5.0;

    @Column("availability_status")
    private DoctorStatus status;
}
