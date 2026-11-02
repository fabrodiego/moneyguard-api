package com.fabrodiego.moneyguard.recurrence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecurrenceRepository extends JpaRepository<Recurrence, Long> {

    @EntityGraph(attributePaths = {"category", "merchant"})
    List<Recurrence> findByUserIdOrderByIdAsc(Long userId);

    @EntityGraph(attributePaths = {"category", "merchant"})
    Optional<Recurrence> findByIdAndUserId(Long id, Long userId);

    /** Ids das assinaturas com cobranca vencida (o lancador processa uma por transacao). */
    @Query("select r.id from Recurrence r where r.active = true and r.nextDueOn is not null and r.nextDueOn <= :today")
    List<Long> findDueIds(LocalDate today);

    @EntityGraph(attributePaths = {"category", "merchant"})
    Optional<Recurrence> findWithRelationsById(Long id);

    boolean existsByCategoryId(Long categoryId);
}
