package com.fabrodiego.moneyguard.merchant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantAliasRepository extends JpaRepository<MerchantAlias, Long> {

    @EntityGraph(attributePaths = {"merchant", "merchant.category"})
    List<MerchantAlias> findByUserId(Long userId);

    Optional<MerchantAlias> findByUserIdAndNormalizedAlias(Long userId, String normalizedAlias);
}
