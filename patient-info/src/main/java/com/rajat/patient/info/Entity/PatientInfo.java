package com.rajat.patient.info.Entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Data
@Table("patient_details")
@AllArgsConstructor
public class PatientInfo {

    @PrimaryKey
    @Column("patient_id")
    UUID patientId ;

    @Column("name")
    String name;

    @Column("email")
    String email;

    @Column("age")
    String age;
}
