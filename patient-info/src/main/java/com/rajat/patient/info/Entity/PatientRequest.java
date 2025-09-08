package com.rajat.patient.info.Entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatientRequest {

    @JsonProperty("patient_name")
    private String name ;

    @JsonProperty("email")
    private String email;

    @JsonProperty("age")
    private String age;
}
