package com.rajat.doctorRepo.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.UUID;

@Data
@PrimaryKeyClass
@AllArgsConstructor
public class DoctorDetailKey implements Serializable {

    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private UUID id;

    @PrimaryKeyColumn(name = "email", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private String email;

    @PrimaryKeyColumn(name = "name", type = PrimaryKeyType.CLUSTERED, ordinal = 2)
    private String name;

}
