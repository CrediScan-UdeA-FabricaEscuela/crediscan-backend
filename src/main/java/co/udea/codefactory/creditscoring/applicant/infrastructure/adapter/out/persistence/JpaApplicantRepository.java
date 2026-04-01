package co.udea.codefactory.creditscoring.applicant.infrastructure.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaApplicantRepository extends JpaRepository<ApplicantJpaEntity, UUID> {

    boolean existsByIdentificationHash(String identificationHash);

    @Query("""
            SELECT a FROM ApplicantJpaEntity a
            WHERE a.identificationHash = :identificationHash
               OR LOWER(a.name) LIKE LOWER(:nameCriteria)
            """)
    Page<ApplicantJpaEntity> searchByHashOrName(
            @Param("identificationHash") String identificationHash,
            @Param("nameCriteria") String nameCriteria,
            Pageable pageable);
}
