package com.docapp.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DoctorDtos {

    private DoctorDtos() {
    }

    public record DoctorResponse(
            Long id,
            String fullName,
            String email,
            String specialization,
            String qualification,
            int experienceYears,
            int consultationFee,
            String bio,
            String availableFrom,
            String availableTo,
            int slotMinutes) {
    }


    public record SlotResponse(String time, boolean available) {
    }

    public record CreateDoctorRequest(
            @NotBlank(message = "Full name is required") String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Email is not valid") String email,
            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters") String password,
            String phone,
            
            @NotBlank(message = "Specialization is required") String specialization,
            String qualification,
            
            @Min(value = 0, message = "Experience cannot be negative") int experienceYears,
            @Min(value = 0, message = "Fee cannot be negative") int consultationFee,
            String bio,
            
            @NotNull(message = "Start time is required") LocalTime availableFrom,
            @NotNull(message = "End time is required") LocalTime availableTo,
            @Min(value = 10, message = "Slot must be at least 10 minutes") int slotMinutes) {
    }
}
