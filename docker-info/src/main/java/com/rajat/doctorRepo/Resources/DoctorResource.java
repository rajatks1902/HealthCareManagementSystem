package com.rajat.doctorRepo.Resources;


import com.rajat.doctorRepo.Entity.Doctor;
import com.rajat.doctorRepo.Entity.DoctorRequest;
import com.rajat.doctorRepo.Entity.GenericResponse;
import com.rajat.doctorRepo.Manager.DoctorManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/doctor")
public class DoctorResource {

    @Autowired
    DoctorManager doctorManager;

    private static final Logger logger = LoggerFactory.getLogger(DoctorResource.class);

    @GetMapping("/{id}")
    public ResponseEntity<?> getDrById(@PathVariable String id){
        try{
            Doctor doctor = doctorManager.getDoctorInfo(id);
            return ResponseEntity.status(200).body(new GenericResponse<>("Dr Fetched" ,doctor));
        }catch (Exception exp){
            logger.error("Error fetching Dr with id {} | error message : {} ",id ,exp.getMessage() );
            return ResponseEntity.status(500).body(new GenericResponse<>("Error in Fetching Dr : Check logs for Details" , exp.getMessage()));
        }

    }

    @PostMapping
    public  ResponseEntity<?> saveNewDoctor(@RequestBody DoctorRequest doctor){
        try{
        UUID doctorUniqueId = doctorManager.addNewDoctor(doctor);
        return ResponseEntity.status(200).body(new GenericResponse<>("Doctor Save Successfully",doctorUniqueId));
    } catch(Exception e) {
        logger.error("Error saving doctor: " + e.getMessage());
        return ResponseEntity.status(500).body(new GenericResponse<>("Error saving doctor"));
    }
    }

    @GetMapping("/all")
    public  ResponseEntity getAllDoctor(){
        List<Doctor> drInfo= doctorManager.getAllDoctorInfo();
        return ResponseEntity.status(200).body(new GenericResponse<>("Dr List Fetched",drInfo));

    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDoctor(@Valid @RequestBody Doctor doctor, @PathVariable String id) throws Exception {
        try {
            doctorManager.updateDoctorById(id, doctor);
            return ResponseEntity.status(200).body(new GenericResponse<>("Doctor updated successfully"));
        } catch(Exception e) {
            logger.error("Error updating doctor: " + e.getMessage());
            return ResponseEntity.status(500).body(new GenericResponse<>("Error updating doctor"));
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable String id) throws Exception {
        try {
            doctorManager.deleteDoctorById(id);
            return ResponseEntity.status(200).body(new GenericResponse<>("Doctor deleted successfully"));
        } catch(Exception e) {
            logger.error("Error deleting doctor: " + e.getMessage());
            return ResponseEntity.status(500).body(new GenericResponse<>("Error deleting doctor"));
        }

    }
}
