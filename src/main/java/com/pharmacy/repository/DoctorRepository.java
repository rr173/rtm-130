package com.pharmacy.repository;

import com.pharmacy.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByDoctorId(String doctorId);

    boolean existsByDoctorId(String doctorId);
}
