package com.pharmacy.repository;

import com.pharmacy.entity.Pharmacist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacistRepository extends JpaRepository<Pharmacist, Long> {

    Optional<Pharmacist> findByPharmacistId(String pharmacistId);

    List<Pharmacist> findByActiveTrue();

    boolean existsByPharmacistId(String pharmacistId);
}
