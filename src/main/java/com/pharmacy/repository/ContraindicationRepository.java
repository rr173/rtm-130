package com.pharmacy.repository;

import com.pharmacy.entity.Contraindication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContraindicationRepository extends JpaRepository<Contraindication, Long> {

    @Query("SELECT c FROM Contraindication c WHERE " +
           "(c.drugACode = :drugA AND c.drugBCode = :drugB) OR " +
           "(c.drugACode = :drugB AND c.drugBCode = :drugA)")
    List<Contraindication> findContraindication(@Param("drugA") String drugA, @Param("drugB") String drugB);

    @Query("SELECT c FROM Contraindication c WHERE " +
           "c.drugACode IN :drugCodes AND c.drugBCode IN :drugCodes")
    List<Contraindication> findAllContraindicationsWithin(@Param("drugCodes") List<String> drugCodes);

    boolean existsByDrugACodeAndDrugBCode(String drugACode, String drugBCode);
}
