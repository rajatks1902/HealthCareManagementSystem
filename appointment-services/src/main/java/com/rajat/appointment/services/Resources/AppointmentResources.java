package com.rajat.appointment.services.Resources;

import com.rajat.appointment.services.model.Appointment;
import com.rajat.appointment.services.payload.request.AppointmentRequest;
import com.rajat.appointment.services.payload.response.AppointmentStatsResponse;
import com.rajat.appointment.services.service.AppointmentService;
import com.rajat.appointment.services.service.AppointmentStatsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentResources {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentResources.class);

    private final AppointmentService appointmentService;
    private final AppointmentStatsService appointmentStatsService;

    public AppointmentResources(AppointmentService appointmentService,
                                AppointmentStatsService appointmentStatsService) {
        this.appointmentService = appointmentService;
        this.appointmentStatsService = appointmentStatsService;
    }

    /**
     * Books a new appointment.
     *
     * @param appointmentRequest the appointment details.
     * @return a response entity containing the result or an error message.
     */
    @PostMapping("/create")
    public ResponseEntity<String> bookAppointment(@Valid @RequestBody AppointmentRequest appointmentRequest) {
        logger.info("Booking appointment: {}", appointmentRequest);
        try {
            String result = appointmentService.bookAppointment(appointmentRequest);
            logger.info("Appointment booked successfully: {}", result);
            return ResponseEntity.ok("Appointment booked successfully. ID: " + result);
        } catch (Exception e) {
            logger.error("Error booking appointment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Unable to book appointment.");
        }
    }

    /**
     * Retrieves appointments for a specific doctor by ID.
     *
     * @param doctorId the unique ID of the doctor.
     * @return a response entity with the list of appointments or an error message.
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getAppointmentsByDoctorId(@PathVariable String doctorId) {
        logger.info("Fetching appointments for doctor ID: {}", doctorId);
        try {
            List<Appointment> appointments = appointmentService.getByDoctorId(doctorId);
            logger.info("Appointments fetched successfully for doctor ID: {}", doctorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            logger.error("Error fetching appointments for doctor ID {}: {}", doctorId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Unable to fetch appointments.");
        }
    }

    /**
     * Retrieves appointments for a specific patient by ID.
     *
     * @param patientId the unique ID of the patient.
     * @return a response entity with the list of appointments or an error message.
     */
//    @GetMapping("/patient/{patientId}")
//    public ResponseEntity<?> getAppointmentsByPatientId(@PathVariable String patientId) {
//        logger.info("Fetching appointments for patient ID: {}", patientId);
//        try {
//            List<Appointment> appointments = appointmentService.getByPatientId(patientId);
//            logger.info("Appointments fetched successfully for patient ID: {}", patientId);
//            return ResponseEntity.ok(appointments);
//        } catch (Exception e) {
//            logger.error("Error fetching appointments for patient ID {}: {}", patientId, e.getMessage(), e);
//            return ResponseEntity.internalServerError().body("Unable to fetch appointments.");
//        }
//    }

    /**
     * Retrieves all appointments.
     *
     * @return a response entity containing all appointments or an error message.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllAppointments() {
        logger.info("Fetching all appointments.");
        try {
            List<Appointment> appointments = appointmentService.getAllAppointments();
            logger.info("All appointments fetched successfully.");
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            logger.error("Error fetching all appointments: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Unable to fetch appointments.");
        }
    }

    /**
     * Updates an existing appointment.
     *
     * @param appointmentRequest the updated appointment details.
     * @return a response entity with the result or an error message.
     */
    @PutMapping
    public ResponseEntity<String> updateAppointment(@Valid @RequestBody AppointmentRequest appointmentRequest) {
        logger.info("Updating appointment: {}", appointmentRequest);
        try {
            String result = appointmentService.updateAppointment(appointmentRequest);
            logger.info("Appointment updated successfully: {}", result);
            return ResponseEntity.ok("Appointment updated successfully. ID: " + result);
        } catch (Exception e) {
            logger.error("Error updating appointment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Unable to update appointment.");
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getAppointmentStats(
            @RequestParam(defaultValue = "DAY") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(defaultValue = "false") boolean refresh) {
        try {
            List<AppointmentStatsResponse> stats = appointmentStatsService.getStats(period, from, to, doctorId, refresh);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Error fetching appointment stats", ex);
            return ResponseEntity.internalServerError().body("Unable to fetch appointment stats.");
        }
    }

    @PostMapping("/stats/rebuild")
    public ResponseEntity<String> rebuildAppointmentStats() {
        try {
            appointmentStatsService.rebuildStats();
            return ResponseEntity.ok("Appointment statistics rebuilt successfully.");
        } catch (Exception ex) {
            logger.error("Error rebuilding appointment stats", ex);
            return ResponseEntity.internalServerError().body("Unable to rebuild appointment stats.");
        }
    }
}
