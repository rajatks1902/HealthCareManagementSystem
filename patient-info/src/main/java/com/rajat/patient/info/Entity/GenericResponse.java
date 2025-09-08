package com.rajat.patient.info.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponse<T> {

    private String message ;

    private T data ;

    public GenericResponse(String message){
        this.message=message;
    }
}
