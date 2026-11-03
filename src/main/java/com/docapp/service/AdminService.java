package com.docapp.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docapp.dto.AuthDtos.UserInfo;
import com.docapp.dto.DoctorDtos.CreateDoctorRequest;
import com.docapp.dto.DoctorDtos.DoctorResponse;
import com.docapp.exception.ApiException;
import com.docapp.model.AppointmentStatus;
import com.docapp.model.Doctor;
import com.docapp.model.Role;
import com.docapp.model.User;
import com.docapp.repository.AppointmentRepository;
import com.docapp.repository.DoctorRepository;
import com.docapp.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        DoctorRepository doctorRepository,
                        AppointmentRepository appointmentRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public DoctorResponse createDoctor(CreateDoctorRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "This email is already registered");
        }
        if (!request.availableFrom().isBefore(request.availableTo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Start time must be before end time");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(Role.DOCTOR);
        userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialization(request.specialization().trim());
        doctor.setQualification(request.qualification());
        doctor.setExperienceYears(request.experienceYears());
        doctor.setConsultationFee(request.consultationFee());
        doctor.setBio(request.bio());
        doctor.setAvailableFrom(request.availableFrom());
        doctor.setAvailableTo(request.availableTo());
        doctor.setSlotMinutes(request.slotMinutes());
        doctorRepository.save(doctor);

        return DoctorService.toResponse(doctor);
    }

    @Transactional(readOnly = true)
    public List<UserInfo> users() {
        return userRepository.findAll().stream().map(AuthService::toInfo).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("patients", userRepository.countByRole(Role.PATIENT));
        stats.put("doctors", userRepository.countByRole(Role.DOCTOR));
        stats.put("totalAppointments", appointmentRepository.count());
        stats.put("booked", appointmentRepository.countByStatus(AppointmentStatus.BOOKED));
        stats.put("confirmed", appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED));
        stats.put("completed", appointmentRepository.countByStatus(AppointmentStatus.COMPLETED));
        stats.put("cancelled", appointmentRepository.countByStatus(AppointmentStatus.CANCELLED));
        return stats;
    }
}
