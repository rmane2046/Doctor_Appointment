package com.docapp.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docapp.dto.DoctorDtos.DoctorResponse;
import com.docapp.dto.DoctorDtos.SlotResponse;
import com.docapp.exception.ApiException;
import com.docapp.model.Appointment;
import com.docapp.model.AppointmentStatus;
import com.docapp.model.Doctor;
import com.docapp.repository.AppointmentRepository;
import com.docapp.repository.DoctorRepository;

@Service
public class DoctorService {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> list(String specialization) {
        List<Doctor> doctors = (specialization == null || specialization.isBlank())
                ? doctorRepository.findAll()
                : doctorRepository.findBySpecializationIgnoreCase(specialization.trim());
        return doctors.stream().map(DoctorService::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DoctorResponse get(Long id) {
        return toResponse(findDoctor(id));
    }

    @Transactional(readOnly = true)
    public List<String> specializations() {
        return doctorRepository.findAllSpecializations();
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> slots(Long doctorId, LocalDate date) {
        Doctor doctor = findDoctor(doctorId);
        List<SlotResponse> result = new ArrayList<>();

        LocalDate today = LocalDate.now(IST);
        if (date.isBefore(today)) {
            return result;
        }

        Set<LocalTime> booked = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndStatusNot(doctorId, date, AppointmentStatus.CANCELLED)
                .stream()
                .map(Appointment::getStartTime)
                .collect(Collectors.toSet());

        LocalTime now = LocalTime.now(IST);
        int slot = doctor.getSlotMinutes();
        int startMinute = doctor.getAvailableFrom().toSecondOfDay() / 60;
        int endMinute = doctor.getAvailableTo().toSecondOfDay() / 60;

        for (int minute = startMinute; minute + slot <= endMinute; minute += slot) {
            LocalTime time = LocalTime.ofSecondOfDay(minute * 60L);
            boolean passed = date.isEqual(today) && !time.isAfter(now);
            boolean available = !booked.contains(time) && !passed;
            result.add(new SlotResponse(time.toString(), available));
        }
        return result;
    }

    Doctor findDoctor(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Doctor not found"));
    }

    public static DoctorResponse toResponse(Doctor d) {
        return new DoctorResponse(
                d.getId(),
                d.getUser().getFullName(),
                d.getUser().getEmail(),
                d.getSpecialization(),
                d.getQualification(),
                d.getExperienceYears(),
                d.getConsultationFee(),
                d.getBio(),
                d.getAvailableFrom().toString(),
                d.getAvailableTo().toString(),
                d.getSlotMinutes());
    }
}
