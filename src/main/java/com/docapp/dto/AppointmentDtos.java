package com.docapp.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.docapp.model.AppointmentStatus;

import jakarta.validation.constraints.NotNull;

public final class AppointmentDtos {

    private AppointmentDtos() 
    {
    }

    public record BookRequest(
            @NotNull(message = "Doctor is required") Long doctorId,
            @NotNull(message = "Date is required") LocalDate date,
            @NotNull(message = "Time is required") LocalTime time,
            String reason) {
    }

    public record StatusRequest(@NotNull(message = "Status is required") AppointmentStatus status) 
    {
    }

    public record AppointmentResponse(
            Long id,
            Long doctorId,
            String doctorName,
            String specialization,
            Long patientId,
            String patientName,
            String patientPhone,
            String date,
            String time,
            String status,
            String reason) {
    }
}
