package com.docapp.repository;

import com.docapp.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    Optional<Doctor> findByUserId(Long userId);

    @Query("select distinct d.specialization from Doctor d order by d.specialization")
    List<String> findAllSpecializations();
}
