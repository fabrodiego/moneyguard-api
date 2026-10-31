package com.fabrodiego.moneyguard.merchant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    @EntityGraph(attributePaths = "category")
    List<Merchant> findByUserIdOrderByNameAsc(Long userId);

    @EntityGraph(attributePaths = "category")
    Optional<Merchant> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "category")
    Optional<Merchant> findByUserIdAndNormalizedName(Long userId, String normalizedName);
}
