package com.rajat.doctorRepo.Manager;

import com.rajat.doctorRepo.Dao.DoctorBasicInfoRepository;
import com.rajat.doctorRepo.Dao.DoctorRepository;
import com.rajat.doctorRepo.Entity.*;
import com.rajat.doctorRepo.Exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.rajat.doctorRepo.Entity.DoctorStatus.AVAILABLE;

@Service
public class DoctorManager {

    @Autowired
    DoctorRepository doctorRepository;

    @Autowired
    DoctorBasicInfoRepository doctorBasicInfoRepository;

    public UUID addNewDoctor(DoctorRequest doctor){
        UUID doctorUniqueId = UUID.randomUUID();
        Doctor newDoctorEntity = new Doctor();
        DoctorDetailKey doctorDetailKey =new DoctorDetailKey(doctorUniqueId,doctor.getEmail(), doctor.getName());
        newDoctorEntity.setDoctorDetailKey(doctorDetailKey);
        newDoctorEntity.setStatus(DoctorStatus.valueOf(doctor.getStatus()));
        newDoctorEntity.setSpeciality(doctor.getSpeciality());
        newDoctorEntity.setYearOfExp(doctor.getYear_of_exp());
        doctorRepository.save(newDoctorEntity);
        doctorBasicInfoRepository.save(new DoctorBasicInfo(doctorUniqueId,doctor.getEmail(), doctor.getName()));
        return doctorUniqueId;

    }

    public Doctor getDoctorInfo(String id) {

        Optional<DoctorBasicInfo> doctorBasicInfo = doctorBasicInfoRepository.findById(UUID.fromString(id));
        if(doctorBasicInfo.isEmpty())
            throw new ResourceNotFoundException("Doctor Info Not Present");

        Optional<Doctor> doctor = doctorRepository.findById(new DoctorDetailKey(UUID.fromString(id),doctorBasicInfo.get().getEmail(),doctorBasicInfo.get().getName()));
        return doctor.get();
    }

    public List<Doctor> getAllDoctorInfo() {

        return  doctorRepository.findAll();
    }

    public void updateDoctorById(String id, @Valid Doctor doctor) {
        Optional<DoctorBasicInfo> doctorBasicInfo = doctorBasicInfoRepository.findById(UUID.fromString(id));
        if(doctorBasicInfo.isEmpty())
            throw new ResourceNotFoundException("Doctor Info Not Present");

        if(doctor.getStatus()!=null){
            doctorRepository.updateDoctorStatus(doctorBasicInfo.get().getId(), doctorBasicInfo.get().getEmail(),doctorBasicInfo.get().getName(), String.valueOf(doctor.getStatus()));
        }else{
            doctorRepository.updateDoctorRating(doctorBasicInfo.get().getId(), doctorBasicInfo.get().getEmail(),doctorBasicInfo.get().getName(),doctor.getRating());
        }

    }

    public void deleteDoctorById(String id) {
        Optional<DoctorBasicInfo> doctorBasicInfo = doctorBasicInfoRepository.findById(UUID.fromString(id));
        if(doctorBasicInfo.isEmpty())
            throw new ResourceNotFoundException("Doctor Info Not Present");

        doctorRepository.deleteById( new DoctorDetailKey(UUID.fromString(id),doctorBasicInfo.get().getEmail(),doctorBasicInfo.get().getName()));
        doctorBasicInfoRepository.deleteById(UUID.fromString(id));

    }
}
