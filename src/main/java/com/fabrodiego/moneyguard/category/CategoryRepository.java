package com.fabrodiego.moneyguard.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fabrodiego.moneyguard.transaction.TransactionType;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByTypeAscNameAsc(Long userId);

    List<Category> findByUserIdAndTypeOrderByNameAsc(Long userId, TransactionType type);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    @Query("""
        select count(c) > 0 from Category c
        where c.userId = :userId and lower(c.name) = lower(:name) and c.type = :type
          and (:excludeId is null or c.id <> :excludeId)
        """)
    boolean existsDuplicate(Long userId, String name, TransactionType type, Long excludeId);
}
