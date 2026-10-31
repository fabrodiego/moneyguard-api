package com.fabrodiego.moneyguard.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @EntityGraph(attributePaths = {"category", "merchant"})
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"category", "merchant"})
    @Query("""
        select t from Transaction t
        where t.userId = :userId and t.occurredOn between :from and :to
          and (:categoryId is null or t.category.id = :categoryId)
          and (:type is null or t.type = :type)
        order by t.occurredOn desc, t.id desc
        """)
    List<Transaction> search(Long userId, LocalDate from, LocalDate to, Long categoryId, TransactionType type);

    /** Valores sao cifrados: as somas sao feitas em memoria por quem chama. */
    @Query("select t.type, t.amount from Transaction t where t.userId = :userId")
    List<Object[]> findAllTypeAndAmount(Long userId);

    boolean existsByCategoryId(Long categoryId);
}
