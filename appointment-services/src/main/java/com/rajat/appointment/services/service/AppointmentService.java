package com.rajat.appointment.services.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rajat.appointment.services.Dao.AppointmentByDoctor;
import com.rajat.appointment.services.Dao.AppointmentDao;
import com.rajat.appointment.services.exception.ResourceNotFoundException;
import com.rajat.appointment.services.model.*;
import com.rajat.appointment.services.payload.event.AppointmentNotificationEvent;
import com.rajat.appointment.services.payload.request.AppointmentRequest;
import com.rajat.appointment.services.payload.response.GenericResponse;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentDao appointmentRepository;
    private final AppointmentByDoctor appointmentByDoctor;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final String doctorServiceUrl;
    private final String patientServiceUrl;

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    public AppointmentService(AppointmentDao appointmentRepository,
                              AppointmentByDoctor appointmentByDoctor,
                              RestTemplate restTemplate,
                              KafkaTemplate<String, String> kafkaTemplate,
                              @Value("${doctor.service.url}") String doctorServiceUrl,
                              @Value("${patient.service.url}") String patientServiceUrl) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentByDoctor = appointmentByDoctor;
        this.restTemplate = restTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.doctorServiceUrl = doctorServiceUrl;
        this.patientServiceUrl = patientServiceUrl;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * Books a new appointment by validating doctor and patient information.
     *
     * @param appointment the appointment request details.
     * @return the ID of the newly created appointment.
     */
    public String bookAppointment(AppointmentRequest appointment) {
        try {

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

            Appointment savedAppointment = appointmentRepository.save(newAppointment);
            appointmentByDoctor.save(new DoctorAppointments(savedAppointment.getDoctorId(), savedAppointment.getId()));
            String appointmentId = String.valueOf(savedAppointment.getId());


            publishAppointmentEvent(newAppointment, doctor, patient);
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
        List<DoctorAppointments> doctorAppointments = appointmentByDoctor.findByDoctorId(uuid);

        List<Appointment> appointments = new ArrayList<>();
        for (DoctorAppointments doctorAppointment : doctorAppointments) {
            appointmentRepository.findById(doctorAppointment.getId()).ifPresent(appointments::add);
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

            Doctor doctor = fetchDoctorDetails(appointment.getDoctorId());
            Patient patient = fetchPatientDetails(appointment.getPatientId());


            // Update appointment details
            existingAppointment.setDoctorId(doctor.getId());
            existingAppointment.setPatientId(patient.getId());
            existingAppointment.setAppointmentTime(appointment.getAppointmentTime());
            existingAppointment.setNotes(appointment.getNotes());
            existingAppointment.setDoctorComments(appointment.getDoctorComments());
            existingAppointment.setStatus(AppointmentStatus.fromValue(appointment.getStatus()));

            String idResponse = String.valueOf(appointmentRepository.save(existingAppointment).getId());
            appointmentByDoctor.save(new DoctorAppointments(existingAppointment.getDoctorId(), existingAppointment.getId()));

            publishAppointmentEvent(existingAppointment, doctor, patient);

            return idResponse;


        } catch (Exception e) {
            throw new ResourceNotFoundException("Error updating appointment: " + e.getMessage());
        }
    }

    private void publishAppointmentEvent(Appointment appointment, Doctor doctor, Patient patient) throws Exception {
        AppointmentNotificationEvent event = new AppointmentNotificationEvent(
                appointment.getId().toString(),
                patient,
                doctor,
                appointment.getAppointmentTime(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getDoctorComments()
        );
        sendEventToKafka(objectMapper.writeValueAsString(event));
    }

    private void sendEventToKafka(String appointmentJson) {
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
                logger.error("Failed to send appointment event to topic: {}", topicName, exception);
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

        GenericResponse<Patient> getPatientResponse = responseEntity.getBody();

        if (getPatientResponse != null && getPatientResponse.getData() != null) {
            return getPatientResponse.getData();
        }

        throw new ResourceNotFoundException("Patient not found with ID: " + patientId);
    }

    private boolean isDevelopmentEnvironment() {
        return  true;
    }
}
