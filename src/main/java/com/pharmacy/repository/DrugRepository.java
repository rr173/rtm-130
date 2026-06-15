package com.pharmacy.repository;

import com.pharmacy.entity.Drug;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {

    Optional<Drug> findByDrugCode(String drugCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Drug d WHERE d.drugCode = :drugCode")
    Optional<Drug> findByDrugCodeWithLock(@Param("drugCode") String drugCode);

    List<Drug> findByDrugCodeIn(List<String> drugCodes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Drug d WHERE d.drugCode IN :drugCodes")
    List<Drug> findByDrugCodesWithLock(@Param("drugCodes") List<String> drugCodes);

    boolean existsByDrugCode(String drugCode);
}
