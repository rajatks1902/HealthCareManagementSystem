package com.rajat.patient.info.Manager;

import com.rajat.patient.info.Dao.PatientDao;
import com.rajat.patient.info.Entity.PatientInfo;
import com.rajat.patient.info.Entity.PatientRequest;
import com.rajat.patient.info.Exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PatientManager {

    @Autowired
    PatientDao patientDao;

    public List<PatientInfo> getAllPatientInfo(){

        return patientDao.findAll();
    }

    public PatientInfo getPatientInfo(String patientId){

        Optional<PatientInfo> patientInfo = patientDao.findById(UUID.fromString(patientId));
        if(patientInfo.isEmpty())
            throw new ResourceNotFoundException("No PatientInfo Present");
        else
            return patientInfo.get();
    }

    public UUID savePatientInfo(PatientRequest patientRequest){
        UUID patientUniqueId = UUID.randomUUID();
        PatientInfo patientInfo = new PatientInfo(patientUniqueId,patientRequest.getName(),patientRequest.getEmail(),patientRequest.getAge());
        patientDao.save(patientInfo);
        return patientUniqueId;
    }
}
