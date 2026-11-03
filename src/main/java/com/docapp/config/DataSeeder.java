package com.docapp.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.docapp.model.Appointment;
import com.docapp.model.AppointmentStatus;
import com.docapp.model.Doctor;
import com.docapp.model.Role;
import com.docapp.model.User;
import com.docapp.repository.AppointmentRepository;
import com.docapp.repository.DoctorRepository;
import com.docapp.repository.UserRepository;

/**
 * Runs once at start-up and inserts demo data if the database is empty.
 *
 * Demo logins:
 *   admin@docapp.com    / Admin@123
 *   patient@docapp.com  / Patient@123
 *   anjali.sharma@docapp.com (and the other doctors) / Doctor@123
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      DoctorRepository doctorRepository,
                      AppointmentRepository appointmentRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded (e.g. when using MySQL)
        }

        createUser("Admin", "admin@docapp.com", "Admin@123", "9000000000", Role.ADMIN);
        User patient = createUser("Demo Patient", "patient@docapp.com", "Patient@123", "9111111111", Role.PATIENT);

        Doctor anjali = createDoctor("Dr. Anjali Sharma", "anjali.sharma@docapp.com", "Cardiology",
                "MBBS, MD (Cardiology)", 12, 800,
                "Heart specialist focused on preventive cardiology and blood pressure management.",
                LocalTime.of(9, 0), LocalTime.of(13, 0), 30);
        createDoctor("Dr. Rahul Verma", "rahul.verma@docapp.com", "Dermatology",
                "MBBS, MD (Dermatology)", 8, 600,
                "Treats acne, hair fall, eczema and other skin conditions.",
                LocalTime.of(10, 0), LocalTime.of(16, 0), 20);
        createDoctor("Dr. Priya Nair", "priya.nair@docapp.com", "Pediatrics",
                "MBBS, DCH", 10, 500,
                "Child specialist: vaccinations, growth monitoring and common childhood illnesses.",
                LocalTime.of(9, 30), LocalTime.of(14, 30), 30);
        createDoctor("Dr. Amit Deshmukh", "amit.deshmukh@docapp.com", "Orthopedics",
                "MBBS, MS (Orthopedics)", 15, 900,
                "Joint, bone and sports-injury specialist.",
                LocalTime.of(11, 0), LocalTime.of(17, 0), 30);
        createDoctor("Dr. Sneha Kulkarni", "sneha.kulkarni@docapp.com", "General Physician",
                "MBBS, MD (General Medicine)", 6, 300,
                "First point of contact for fever, infections, diabetes and general health check-ups.",
                LocalTime.of(9, 0), LocalTime.of(18, 0), 15);
        createDoctor("Dr. Vikram Patil", "vikram.patil@docapp.com", "Neurology",
                "MBBS, DM (Neurology)", 14, 1000,
                "Headache, migraine, epilepsy and stroke care.",
                LocalTime.of(14, 0), LocalTime.of(19, 0), 30);

        // one sample appointment for tomorrow so the dashboards are not empty
        Appointment sample = new Appointment();
        sample.setPatient(patient);
        sample.setDoctor(anjali);
        sample.setAppointmentDate(LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1));
        sample.setStartTime(LocalTime.of(10, 0));
        sample.setStatus(AppointmentStatus.BOOKED);
        sample.setReason("Routine heart check-up");
        sample.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        appointmentRepository.save(sample);
    }

    private User createUser(String name, String email, String rawPassword, String phone, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setRole(role);
        return userRepository.save(user);
    }

    private Doctor createDoctor(String name, String email, String specialization, String qualification,
                                int experienceYears, int fee, String bio,
                                LocalTime from, LocalTime to, int slotMinutes) {
        User user = createUser(name, email, "Doctor@123", "9222222222", Role.DOCTOR);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialization(specialization);
        doctor.setQualification(qualification);
        doctor.setExperienceYears(experienceYears);
        doctor.setConsultationFee(fee);
        doctor.setBio(bio);
        doctor.setAvailableFrom(from);
        doctor.setAvailableTo(to);
        doctor.setSlotMinutes(slotMinutes);
        return doctorRepository.save(doctor);
    }
}
