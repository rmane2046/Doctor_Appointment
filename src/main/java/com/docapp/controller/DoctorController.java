package com.docapp.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.docapp.dto.DoctorDtos.DoctorResponse;
import com.docapp.dto.DoctorDtos.SlotResponse;
import com.docapp.service.DoctorService;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) 
    {
        this.doctorService = doctorService;
    }

    @GetMapping
    public List<DoctorResponse> list(@RequestParam(required = false) String specialization) 
    {
        return doctorService.list(specialization);
    }

    @GetMapping("/specializations")
    public List<String> specializations() 
    {
        return doctorService.specializations();
    }

    @GetMapping("/{id}")
    public DoctorResponse get(@PathVariable Long id) 
    {
        return doctorService.get(id);
    }

    @GetMapping("/{id}/slots")
    public List<SlotResponse> slots(@PathVariable Long id,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) 
    {
        return doctorService.slots(id, date);
    }
}
