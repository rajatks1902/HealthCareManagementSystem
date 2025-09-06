package com.rajat.doctorRepo.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DoctorStatus {
    AVAILABLE,
    NOT_AVAILABLE,
    DISABLED;

    @JsonCreator
    public static DoctorStatus fromValue(String value) {
//        if(value == null || value.isBlank()) {
//            throw new InvalidDoctorStatusException("Status field is required");
//        }
        for (DoctorStatus status : DoctorStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
        // Throw a custom exception for invalid values
//        throw new InvalidDoctorStatusException("Invalid doctor status value " + value);
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
