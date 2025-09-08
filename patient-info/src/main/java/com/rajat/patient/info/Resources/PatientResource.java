package com.rajat.patient.info.Resources;


import com.rajat.patient.info.Entity.GenericResponse;
import com.rajat.patient.info.Entity.PatientInfo;
import com.rajat.patient.info.Entity.PatientRequest;
import com.rajat.patient.info.Manager.PatientManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patient")
public class PatientResource {

    @Autowired
    PatientManager patientManager;


    @GetMapping()
    public ResponseEntity<?> getAllPatient(){

        try{
            List<PatientInfo> patientInfoList = patientManager.getAllPatientInfo();
            return  ResponseEntity.status(200).body(new GenericResponse<>("All PatientList",patientInfoList));
        } catch (RuntimeException e) {
            return  ResponseEntity.status(500).body(new GenericResponse<>("Error in Fetching Patient Info"));
        }
    }

    @PostMapping()
    public ResponseEntity<?> savePatient(@RequestBody PatientRequest patientRequest){
        try{
            UUID patientUniqueId =patientManager.savePatientInfo(patientRequest);
            return  ResponseEntity.status(200).body(new GenericResponse<>("New Patient Account Created",patientUniqueId));
        }catch (RuntimeException ex){
            return ResponseEntity.status(500).body(new GenericResponse<>("Error in Creating New Patient",patientRequest));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPatientInfoById(@PathVariable String id){
        try{
            PatientInfo patientInfo = patientManager.getPatientInfo(id);
            return  ResponseEntity.status(200).body(new GenericResponse<>("Patient Info ",patientInfo));
        } catch (RuntimeException e) {
            return  ResponseEntity.status(500).body(new GenericResponse<>("Patient Did Not Exist",id));
        }
    }
}
