package com.rajat.doctorRepo.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DoctorRequest {

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    @JsonProperty("speciality")
    private String speciality;

    @JsonProperty("year_of_exp")
    private Double year_of_exp;

    @JsonProperty("rating")
    private Double rating;

    @JsonProperty("status")
    private String status;
}
