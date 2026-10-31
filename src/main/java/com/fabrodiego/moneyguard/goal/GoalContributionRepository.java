package com.fabrodiego.moneyguard.goal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    List<GoalContribution> findByGoalIdOrderByContributedOnDescIdDesc(Long goalId);

    Optional<GoalContribution> findByIdAndGoalId(Long id, Long goalId);

    /** [goalId, amount] de todos os aportes do usuario; a soma e feita em memoria (valores cifrados). */
    @Query("select c.goal.id, c.amount from GoalContribution c where c.goal.userId = :userId")
    List<Object[]> findAllGoalIdAndAmount(Long userId);
}
