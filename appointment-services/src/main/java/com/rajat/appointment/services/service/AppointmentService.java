package com.rajat.appointment.services.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rajat.appointment.services.Dao.AppointmentByDoctor;
import com.rajat.appointment.services.Dao.AppointmentDao;
import com.rajat.appointment.services.exception.ResourceNotFoundException;
import com.rajat.appointment.services.model.*;
import com.rajat.appointment.services.payload.request.AppointmentRequest;
import com.rajat.appointment.services.payload.response.GenericResponse;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service class to handle appointment-related business logic.
 * Manages booking, retrieval, and updating of appointments.
 */
@Service
public class AppointmentService {

    @Autowired
    private AppointmentDao appointmentRepository;

    @Autowired
    private AppointmentByDoctor appointmentByDoctor;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${doctor.service.url}")
    private String doctorServiceUrl;

    @Value("${patient.service.url}")
    private String patientServiceUrl;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);
    /**
     * Books a new appointment by validating doctor and patient information.
     *
     * @param appointment the appointment request details.
     * @return the ID of the newly created appointment.
     */
    public String bookAppointment(AppointmentRequest appointment) {
        try {

            // Serialize Appointment object to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            // Fetch and validate doctor details
            // Replace with Actual Doctor Details
            Doctor doctor = fetchDoctorDetails(appointment.getDoctorId());
            logger.info("Fetched doctor details inside book appointment: {}", objectMapper.writeValueAsString(doctor));

            // Fetch and validate patient details
            //Replace with Actual Patient Details
            Patient patient = fetchPatientDetails(appointment.getPatientId());
            logger.info("Fetched patient details inside book appointment: {}", objectMapper.writeValueAsString(patient));

            // Create and save the appointment
            UUID generatedId = UUID.randomUUID();
            Appointment newAppointment = new Appointment();
            newAppointment.setId(generatedId);
            newAppointment.setDoctorId(doctor.getId());
            newAppointment.setPatientId(patient.getId());
            newAppointment.setAppointmentTime(appointment.getAppointmentTime());
            newAppointment.setDoctorComments(appointment.getDoctorComments());
            newAppointment.setNotes(appointment.getNotes());
            newAppointment.setStatus(AppointmentStatus.PENDING.toString());

            String appointmentId = String.valueOf(appointmentRepository.save(newAppointment).getId());


            String appointmentJson = objectMapper.writeValueAsString(newAppointment);
            sendEventToKafka(appointmentJson);
            return appointmentId;
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error booking appointment: " + e.getMessage());
        }
    }

    /**
     * Retrieves appointments by doctor ID, sorted by appointment time.
     *
     * @param doctorId the ID of the doctor.
     * @return a list of appointments for the specified doctor.
     */
    public List<Appointment> getByDoctorId(String doctorId) {
        UUID uuid = UUID.fromString(doctorId);
        Optional<DoctorAppointments> doctorAppointments = appointmentByDoctor.findById(uuid);

        List<Appointment> appointments = new ArrayList<>();
        List<DoctorAppointments> allAppointsID = (List<DoctorAppointments>) doctorAppointments.get();
        for(int i=0;i<allAppointsID.size();i++){
            Optional<Appointment> appointment = appointmentRepository.findById(allAppointsID.get(i).getId());
            if(!appointment.isEmpty())
                appointments.add(appointment.get());
        }
        return appointments;
    }


    /**
     * Retrieves all appointments, sorted by appointment time in ascending order.
     *
     * @return a list of all appointments.
     */
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    /**
     * Updates an existing appointment.
     *
     * @param appointment the updated appointment details.
     * @return the ID of the updated appointment.
     * @throws ResourceNotFoundException if the appointment is not found.
     */
    public String updateAppointment(AppointmentRequest appointment) {
        try {
            // Find the existing appointment
            Appointment existingAppointment = appointmentRepository
                    .findById(UUID.fromString(appointment.getId()))
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Appointment not found with ID: " + appointment.getId())
                    );

            // Fetch and validate doctor details
            Map<String, Object> doctor = restTemplate.getForObject(doctorServiceUrl + "/" + appointment.getDoctorId(), Map.class);
            if (doctor == null || doctor.isEmpty()) {
                throw new ResourceNotFoundException("Doctor not found with ID: " + appointment.getDoctorId());
            }

            // Fetch and validate patient details
            Map<String, Object>  patient = restTemplate.getForObject(patientServiceUrl + "/" + appointment.getPatientId(), Map.class);
            if (patient == null || patient.isEmpty()) {
                throw new ResourceNotFoundException("Patient not found with ID: " + appointment.getPatientId());
            }

            Map<String, Object> doctorData = (Map<String, Object>) doctor.get("data");

            // Manually create a Doctor object from the Map
            Doctor doctor1 = new Doctor();
            doctor1.setId((UUID) doctorData.get("id"));
            doctor1.setFirstName((String) doctorData.get("firstName"));
            doctor1.setLastName((String) doctorData.get("lastName"));
            doctor1.setEmail((String) doctorData.get("email"));
            doctor1.setPhone((String) doctorData.get("phone"));
            doctor1.setSpeciality((String) doctorData.get("speciality"));
            doctor1.setYearsOfExperience((Integer) doctorData.get("yearsOfExperience"));
            doctor1.setStatus((String) doctorData.get("status"));


            Map<String, Object> patientData = (Map<String, Object>) patient.get("data");

            // Manually create a Doctor object from the Map
            Patient patient1 = new Patient();
            patient1.setId((UUID) patientData.get("id"));
            patient1.setFirstName((String) patientData.get("firstName"));
            patient1.setLastName((String) patientData.get("lastName"));
            patient1.setEmail((String) patientData.get("email"));
            patient1.setPhone((String) patientData.get("phone"));
            patient1.setAge((Integer) patientData.get("age"));


            // Update appointment details
            existingAppointment.setDoctorId(doctor1.getId());
            existingAppointment.setPatientId(patient1.getId());
            existingAppointment.setAppointmentTime(appointment.getAppointmentTime());
            existingAppointment.setNotes(appointment.getNotes());
            existingAppointment.setDoctorComments(appointment.getDoctorComments());
            existingAppointment.setStatus(AppointmentStatus.fromValue(appointment.getStatus()));

            String idResponse = String.valueOf(appointmentRepository.save(existingAppointment).getId());

            // Serialize Appointment object to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            String appointmentJson = objectMapper.writeValueAsString(existingAppointment);
            sendEventToKafka(appointmentJson);

            return idResponse;


        } catch (Exception e) {
            throw new ResourceNotFoundException("Error updating appointment: " + e.getMessage());
        }
    }

    private void sendEventToKafka(String appointmentJson) {
        // send message to kafka
        // Send message to Kafka and handle the callback
        // Send the message to Kafka and get a CompletableFuture
        CompletableFuture<SendResult<String, String>> completableFuture = kafkaTemplate.send(topicName, appointmentJson);

        // Add callbacks to handle success and failure
        completableFuture.whenComplete((result, exception) -> {
            if (exception == null) {
                // Success case
                RecordMetadata metadata = result.getRecordMetadata();
                logger.info("Message sent successfully to topic: {}", topicName);
                logger.info("Partition: {}, Offset: {}", metadata.partition(), metadata.offset());
            } else {
                // Failure case
                logger.error("Failed to send message to topic: {}", topicName);
                logger.error(exception.getMessage());
//                    exception.printStackTrace();
            }
        });
    }

    private Doctor fetchDoctorDetails(UUID doctorId) {
        if (isDevelopmentEnvironment()) {
            // Return a mock Doctor object
            return new Doctor(
                    doctorId,
                    "John",
                    "Doe",
                    "doctorhungrycoders@gmail.com",
                    "1234567890",
                    "Cardiology",
                    10,
                    "ACTIVE"
            );
        }

        ResponseEntity<GenericResponse<Doctor>> responseEntity = restTemplate.exchange(
                doctorServiceUrl + "/" + doctorId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<GenericResponse<Doctor>>() {}
        );

        GenericResponse<Doctor> getDoctorResponse = responseEntity.getBody();

        if (getDoctorResponse != null && getDoctorResponse.getData() != null) {
            return getDoctorResponse.getData();
        }

        throw new ResourceNotFoundException("Doctor not found with ID: " + doctorId);

    }

    private Patient fetchPatientDetails(UUID patientId) {
        if (isDevelopmentEnvironment()) {
            // Return a mock Patient object
            return new Patient(
                    patientId,
                    "Jane",
                    "Smith",
                    "jane.smith@example.com",
                    "0987654321",
                    30
            );
        }

        // Actual call in non-development environments
        ResponseEntity<GenericResponse<Patient>> responseEntity = restTemplate.exchange(
                patientServiceUrl + "/" + patientId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<GenericResponse<Patient>>() {}
        );

        GenericResponse<Patient> getDoctorResponse = responseEntity.getBody();

        if (getDoctorResponse != null && getDoctorResponse.getData() != null) {
            return getDoctorResponse.getData();
        }

        throw new ResourceNotFoundException("Doctor not found with ID: " + patientId);
    }

    private boolean isDevelopmentEnvironment() {
        return  true;
    }
}
