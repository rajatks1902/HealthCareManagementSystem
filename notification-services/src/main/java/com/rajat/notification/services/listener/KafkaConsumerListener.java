package com.rajat.notification.services.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rajat.notification.services.model.Appointment;
import com.rajat.notification.services.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer Listener to process messages from the Kafka topic.
 */
@Component
public class KafkaConsumerListener {

	// Logger for debugging and monitoring
	private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerListener.class);

	// ObjectMapper for JSON serialization and deserialization
	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private final EmailService emailService;
	private final String topicName;

	public KafkaConsumerListener(EmailService emailService,
								 @Value("${spring.kafka.topic.name}") String topicName) {
		this.emailService = emailService;
		this.topicName = topicName;
	}

	/**
	 * Kafka listener method to process messages from the specified topic and partition.
	 *
	 * @param message JSON message received from the Kafka topic.
	 */
	@KafkaListener(topics = "#{'${spring.kafka.topic.name}'}")
	public void listen(String message) {
		try {

			logger.info("Received appointment notification event from topic: {}", topicName);

			// Deserialize the JSON message into an Appointment object
			Appointment appointment = objectMapper.readValue(message, Appointment.class);

			// Log the deserialized Appointment object
			logger.info("Received Appointment | rawMessage: {}, parsedJson: {}", message, appointment);

			// Trigger email notifications based on appointment details
			emailService.triggerEmailNotification(appointment);

			logger.info("Email notification sent for Appointment ID: {}", appointment.getId());
		} catch (JsonProcessingException e) {
			// Handle JSON parsing errors
			logger.error("Error deserializing message: {}", message, e);
		} catch (Exception ex) {
			// Catch any other unexpected exceptions
			logger.error("Unexpected error while processing message: {}", message, ex);
		}
	}
}
