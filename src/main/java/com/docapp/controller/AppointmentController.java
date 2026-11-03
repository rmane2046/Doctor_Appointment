package com.docapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docapp.dto.AppointmentDtos.AppointmentResponse;
import com.docapp.dto.AppointmentDtos.BookRequest;
import com.docapp.dto.AppointmentDtos.StatusRequest;
import com.docapp.model.User;
import com.docapp.service.AppointmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) 
    {
        this.appointmentService = appointmentService;
    }

    // PATIENT: book a slot.
    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@AuthenticationPrincipal User user,
                                                    @Valid @RequestBody BookRequest request) 
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.book(user, request));
    }

    // PATIENT: my appointments.
    @GetMapping("/my")
    public List<AppointmentResponse> my(@AuthenticationPrincipal User user)
    {
        return appointmentService.forPatient(user);
    }

    // DOCTOR: appointments of the logged-in doctor.
    @GetMapping("/doctor")
    public List<AppointmentResponse> forDoctor(@AuthenticationPrincipal User user)
    {
        return appointmentService.forDoctor(user);
    }

    // PATIENT / DOCTOR / ADMIN: cancel (ownership is checked in the service). */
    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@AuthenticationPrincipal User user, @PathVariable Long id) 
    {
        return appointmentService.cancel(user, id);
    }

    // DOCTOR / ADMIN: confirm or complete.
    @PatchMapping("/{id}/status")
    public AppointmentResponse updateStatus(@AuthenticationPrincipal User user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody StatusRequest request) 
    {
        return appointmentService.updateStatus(user, id, request.status());
    }
}
