package com.docapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docapp.dto.AppointmentDtos.AppointmentResponse;
import com.docapp.dto.AuthDtos.UserInfo;
import com.docapp.dto.DoctorDtos.CreateDoctorRequest;
import com.docapp.dto.DoctorDtos.DoctorResponse;
import com.docapp.service.AdminService;
import com.docapp.service.AppointmentService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AppointmentService appointmentService;

    public AdminController(AdminService adminService, AppointmentService appointmentService) 
    {
        this.adminService = adminService;
        this.appointmentService = appointmentService;
    }

    @PostMapping("/doctors")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) 
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDoctor(request));
    }

    @GetMapping("/appointments")
    public List<AppointmentResponse> appointments() 
    {
        return appointmentService.all();
    }

    @GetMapping("/users")
    public List<UserInfo> users() 
    {
        return adminService.users();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() 
    {
        return adminService.stats();
    }
}
