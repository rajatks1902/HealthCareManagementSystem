package com.rajat.appointment.services.service;

import com.rajat.appointment.services.Dao.AppointmentDao;
import com.rajat.appointment.services.Dao.AppointmentStatsRepository;
import com.rajat.appointment.services.model.Appointment;
import com.rajat.appointment.services.model.AppointmentStats;
import com.rajat.appointment.services.model.AppointmentStatsKey;
import com.rajat.appointment.services.model.AppointmentStatus;
import com.rajat.appointment.services.payload.response.AppointmentStatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AppointmentStatsService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentStatsService.class);
    private static final String PERIOD_DAY = "DAY";
    private static final String PERIOD_WEEK = "WEEK";

    private final AppointmentDao appointmentRepository;
    private final AppointmentStatsRepository statsRepository;

    public AppointmentStatsService(AppointmentDao appointmentRepository,
                                   AppointmentStatsRepository statsRepository) {
        this.appointmentRepository = appointmentRepository;
        this.statsRepository = statsRepository;
    }

    @Scheduled(cron = "${stats.scheduler.cron:0 0 1 * * *}")
    public void rebuildStatsOnSchedule() {
        rebuildStats();
    }

    public List<AppointmentStatsResponse> getStats(String periodType,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   UUID doctorId,
                                                   boolean refresh) {
        if (refresh || statsRepository.count() == 0) {
            rebuildStats();
        }

        String normalizedPeriod = normalizePeriod(periodType);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : defaultFrom(normalizedPeriod, resolvedTo);
        String requestedDoctor = doctorId != null ? doctorId.toString() : AppointmentStats.ALL_DOCTORS;

        return statsRepository.findAll().stream()
                .filter(stats -> normalizedPeriod.equals(stats.getKey().getPeriodType()))
                .filter(stats -> requestedDoctor.equals(stats.getKey().getDoctorId()))
                .filter(stats -> !stats.getKey().getPeriodStart().isBefore(resolvedFrom))
                .filter(stats -> !stats.getKey().getPeriodStart().isAfter(resolvedTo))
                .sorted(Comparator.comparing(stats -> stats.getKey().getPeriodStart()))
                .map(this::toResponse)
                .toList();
    }

    public synchronized void rebuildStats() {
        List<Appointment> appointments = appointmentRepository.findAll();
        Map<AppointmentStatsKey, StatsAccumulator> aggregate = new LinkedHashMap<>();

        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentTime() == null) {
                logger.warn("Skipping appointment {} because appointmentTime is null", appointment.getId());
                continue;
            }

            LocalDate appointmentDate = appointment.getAppointmentTime().toLocalDate();
            LocalDate weekStart = appointmentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            String doctorId = appointment.getDoctorId() != null ? appointment.getDoctorId().toString() : AppointmentStats.ALL_DOCTORS;

            addAppointment(aggregate, PERIOD_DAY, appointmentDate, AppointmentStats.ALL_DOCTORS, appointment);
            addAppointment(aggregate, PERIOD_DAY, appointmentDate, doctorId, appointment);
            addAppointment(aggregate, PERIOD_WEEK, weekStart, AppointmentStats.ALL_DOCTORS, appointment);
            addAppointment(aggregate, PERIOD_WEEK, weekStart, doctorId, appointment);
        }

        statsRepository.deleteAll();
        statsRepository.saveAll(toStatsRows(aggregate));
        logger.info("Rebuilt appointment statistics from {} appointments into {} rollup rows",
                appointments.size(), aggregate.size());
    }

    private void addAppointment(Map<AppointmentStatsKey, StatsAccumulator> aggregate,
                                String periodType,
                                LocalDate periodStart,
                                String doctorId,
                                Appointment appointment) {
        AppointmentStatsKey key = new AppointmentStatsKey(periodType, periodStart, doctorId);
        aggregate.computeIfAbsent(key, unused -> new StatsAccumulator()).add(appointment);
    }

    private List<AppointmentStats> toStatsRows(Map<AppointmentStatsKey, StatsAccumulator> aggregate) {
        List<AppointmentStats> rows = new ArrayList<>();
        aggregate.forEach((key, count) -> rows.add(new AppointmentStats(
                key,
                periodEnd(key),
                count.total,
                count.pending,
                count.confirmed,
                count.rejected,
                count.completed,
                count.other
        )));
        return rows;
    }

    private LocalDate periodEnd(AppointmentStatsKey key) {
        if (PERIOD_WEEK.equals(key.getPeriodType())) {
            return key.getPeriodStart().plusDays(6);
        }
        return key.getPeriodStart();
    }

    private AppointmentStatsResponse toResponse(AppointmentStats stats) {
        return new AppointmentStatsResponse(
                stats.getKey().getPeriodType(),
                stats.getKey().getPeriodStart(),
                stats.getPeriodEnd(),
                stats.getKey().getDoctorId(),
                stats.getTotalCount(),
                stats.getPendingCount(),
                stats.getConfirmedCount(),
                stats.getRejectedCount(),
                stats.getCompletedCount(),
                stats.getOtherCount()
        );
    }

    private String normalizePeriod(String periodType) {
        if (periodType == null || periodType.isBlank()) {
            return PERIOD_DAY;
        }

        String normalized = periodType.trim().toUpperCase(Locale.ROOT);
        if (!PERIOD_DAY.equals(normalized) && !PERIOD_WEEK.equals(normalized)) {
            throw new IllegalArgumentException("period must be DAY or WEEK");
        }
        return normalized;
    }

    private LocalDate defaultFrom(String periodType, LocalDate to) {
        if (PERIOD_WEEK.equals(periodType)) {
            return to.minusWeeks(8);
        }
        return to.minusDays(7);
    }

    private static class StatsAccumulator {
        private long total;
        private long pending;
        private long confirmed;
        private long rejected;
        private long completed;
        private long other;

        private void add(Appointment appointment) {
            total++;
            String status = appointment.getStatus() == null ? "" : appointment.getStatus().toUpperCase(Locale.ROOT);

            if (AppointmentStatus.PENDING.name().equals(status)) {
                pending++;
            } else if (AppointmentStatus.CONFIRMED.name().equals(status)) {
                confirmed++;
            } else if (AppointmentStatus.REJECTED.name().equals(status)) {
                rejected++;
            } else if (AppointmentStatus.COMPLETED.name().equals(status)) {
                completed++;
            } else {
                other++;
            }
        }
    }
}
