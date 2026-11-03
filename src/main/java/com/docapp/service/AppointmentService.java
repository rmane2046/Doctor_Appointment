package com.docapp.service;

import com.docapp.dto.AppointmentDtos.AppointmentResponse;
import com.docapp.dto.AppointmentDtos.BookRequest;
import com.docapp.exception.ApiException;
import com.docapp.model.Appointment;
import com.docapp.model.AppointmentStatus;
import com.docapp.model.Doctor;
import com.docapp.model.Role;
import com.docapp.model.User;
import com.docapp.repository.AppointmentRepository;
import com.docapp.repository.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public AppointmentResponse book(User patient, BookRequest request) {
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor not found"));

        LocalTime time = request.time().withSecond(0).withNano(0);
        LocalDate today = LocalDate.now(DoctorService.IST);

        if (request.date().isBefore(today)) {
            throw bad("You cannot book an appointment in the past");
        }
        if (request.date().isEqual(today) && !time.isAfter(LocalTime.now(DoctorService.IST))) {
            throw bad("That time has already passed today");
        }
        if (!isValidSlot(doctor, time)) {
            throw bad("Selected time is not a valid slot for this doctor");
        }

        boolean alreadyTaken = appointmentRepository.existsByDoctorIdAndAppointmentDateAndStartTimeAndStatusNot(
                doctor.getId(), request.date(), time, AppointmentStatus.CANCELLED);
        if (alreadyTaken) {
            throw new ApiException(HttpStatus.CONFLICT, "This slot has just been booked by someone else");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.date());
        appointment.setStartTime(time);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setReason(request.reason());
        appointment.setCreatedAt(LocalDateTime.now(DoctorService.IST));
        return toResponse(appointmentRepository.save(appointment));
    }

    private boolean isValidSlot(Doctor doctor, LocalTime time) {
        int slot = doctor.getSlotMinutes();
        int start = doctor.getAvailableFrom().toSecondOfDay() / 60;
        int end = doctor.getAvailableTo().toSecondOfDay() / 60;
        int minute = time.toSecondOfDay() / 60;
        return minute >= start && minute + slot <= end && (minute - start) % slot == 0;
    }

    // ------------------------------------------------------------------ reading

    @Transactional(readOnly = true)
    public List<AppointmentResponse> forPatient(User patient) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescStartTimeDesc(patient.getId())
                .stream().map(AppointmentService::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> forDoctor(User doctorUser) {
        Doctor doctor = doctorRepository.findByUserId(doctorUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor profile not found"));
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateDescStartTimeDesc(doctor.getId())
                .stream().map(AppointmentService::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> all() {
        return appointmentRepository.findAllByOrderByAppointmentDateDescStartTimeDesc()
                .stream().map(AppointmentService::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponse cancel(User actor, Long appointmentId) {
        Appointment appointment = find(appointmentId);

        boolean isOwnerPatient = appointment.getPatient().getId().equals(actor.getId());
        boolean isOwnerDoctor = appointment.getDoctor().getUser().getId().equals(actor.getId());
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isOwnerPatient && !isOwnerDoctor && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot cancel this appointment");
        }

        ensureStillOpen(appointment);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return toResponse(appointmentRepository.save(appointment));
    }

    /** Only the doctor of the appointment (or an admin) may confirm / complete it. */
    @Transactional
    public AppointmentResponse updateStatus(User actor, Long appointmentId, AppointmentStatus newStatus) {
        Appointment appointment = find(appointmentId);

        boolean isOwnerDoctor = appointment.getDoctor().getUser().getId().equals(actor.getId());
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isOwnerDoctor && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This appointment does not belong to you");
        }

        if (newStatus == AppointmentStatus.BOOKED) {
            throw bad("An appointment cannot be moved back to BOOKED");
        }
        ensureStillOpen(appointment);
        appointment.setStatus(newStatus);
        return toResponse(appointmentRepository.save(appointment));
    }

    // ------------------------------------------------------------------ helpers

    private Appointment find(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    private void ensureStillOpen(Appointment appointment) {
        AppointmentStatus current = appointment.getStatus();
        if (current == AppointmentStatus.COMPLETED || current == AppointmentStatus.CANCELLED) {
            throw bad("This appointment is already " + current.name().toLowerCase());
        }
    }

    private ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getDoctor().getId(),
                a.getDoctor().getUser().getFullName(),
                a.getDoctor().getSpecialization(),
                a.getPatient().getId(),
                a.getPatient().getFullName(),
                a.getPatient().getPhone(),
                a.getAppointmentDate().toString(),
                a.getStartTime().toString(),
                a.getStatus().name(),
                a.getReason());
    }
}
