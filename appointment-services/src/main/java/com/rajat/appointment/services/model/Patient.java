
package com.rajat.appointment.services.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents a patient entity with personal details.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Patient {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Integer age;
}