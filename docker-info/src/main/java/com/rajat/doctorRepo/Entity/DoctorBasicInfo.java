package com.rajat.doctorRepo.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Data
@Table("doctor_info")
@AllArgsConstructor
public class DoctorBasicInfo {

    @PrimaryKey
    private UUID id;

    @Column("email")
    private String email;

    @Column("name")
    private String name;
}
