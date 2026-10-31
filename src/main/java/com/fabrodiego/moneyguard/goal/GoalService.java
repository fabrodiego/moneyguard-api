package com.fabrodiego.moneyguard.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.common.BadRequestException;
import com.fabrodiego.moneyguard.common.Money;
import com.fabrodiego.moneyguard.common.NotFoundException;
import com.fabrodiego.moneyguard.goal.GoalDtos.ContributionRequest;
import com.fabrodiego.moneyguard.goal.GoalDtos.ContributionResponse;
import com.fabrodiego.moneyguard.goal.GoalDtos.GoalRequest;
import com.fabrodiego.moneyguard.goal.GoalDtos.GoalResponse;
import com.fabrodiego.moneyguard.goal.GoalDtos.GoalSummary;
import com.fabrodiego.moneyguard.transaction.TransactionService;

@Service
public class GoalService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final GoalRepository goals;
    private final GoalContributionRepository contributions;
    private final TransactionService transactions;
    private final Clock clock;

    public GoalService(GoalRepository goals, GoalContributionRepository contributions,
                       TransactionService transactions, Clock clock) {
        this.goals = goals;
        this.contributions = contributions;
        this.transactions = transactions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> list(Long userId, boolean includeArchived) {
        List<Goal> found = includeArchived
            ? goals.findByUserIdOrderByPriorityAscIdAsc(userId)
            : goals.findByUserIdAndArchivedFalseOrderByPriorityAscIdAsc(userId);
        Map<Long, BigDecimal> saved = savedByGoal(userId);
        return found.stream().map(g -> toResponse(g, saved.getOrDefault(g.getId(), BigDecimal.ZERO))).toList();
    }

    @Transactional(readOnly = true)
    public GoalSummary summary(Long userId) {
        Map<Long, BigDecimal> saved = savedByGoal(userId);
        BigDecimal allocated = Money.of(saved.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal balance = transactions.allTimeBalance(userId);
        List<GoalResponse> active = goals.findByUserIdAndArchivedFalseOrderByPriorityAscIdAsc(userId).stream()
            .map(g -> toResponse(g, saved.getOrDefault(g.getId(), BigDecimal.ZERO))).toList();
        return new GoalSummary(balance, allocated, Money.of(balance.subtract(allocated)), active);
    }

    @Transactional(readOnly = true)
    public GoalResponse get(Long userId, Long id) {
        Goal goal = find(userId, id);
        return toResponse(goal, savedOf(goal.getId()));
    }

    @Transactional
    public GoalResponse create(Long userId, GoalRequest req) {
        int priority = req.priority() != null ? req.priority() : 1;
        Goal goal = goals.save(new Goal(userId, req.name().trim(), Money.of(req.target()), req.deadline(), priority));
        return toResponse(goal, BigDecimal.ZERO);
    }

    @Transactional
    public GoalResponse update(Long userId, Long id, GoalRequest req) {
        Goal goal = find(userId, id);
        int priority = req.priority() != null ? req.priority() : goal.getPriority();
        boolean archived = req.archived() != null ? req.archived() : goal.isArchived();
        goal.update(req.name().trim(), Money.of(req.target()), req.deadline(), priority, archived);
        return toResponse(goal, savedOf(goal.getId()));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        goals.delete(find(userId, id));
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> listContributions(Long userId, Long goalId) {
        Goal goal = find(userId, goalId);
        return contributions.findByGoalIdOrderByContributedOnDescIdDesc(goal.getId()).stream()
            .map(ContributionResponse::from).toList();
    }

    @Transactional
    public ContributionResponse addContribution(Long userId, Long goalId, ContributionRequest req) {
        if (req.amount().signum() == 0) {
            throw new BadRequestException("amount nao pode ser zero");
        }
        Goal goal = find(userId, goalId);
        String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
        LocalDate date = req.contributedOn() != null ? req.contributedOn() : LocalDate.now(clock);
        return ContributionResponse.from(contributions.save(
            new GoalContribution(goal, Money.of(req.amount()), note, date)));
    }

    @Transactional
    public void deleteContribution(Long userId, Long goalId, Long contributionId) {
        Goal goal = find(userId, goalId);
        GoalContribution contribution = contributions.findByIdAndGoalId(contributionId, goal.getId())
            .orElseThrow(() -> new NotFoundException("Aporte nao encontrado"));
        contributions.delete(contribution);
    }

    private Goal find(Long userId, Long id) {
        return goals.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Meta nao encontrada"));
    }

    private BigDecimal savedOf(Long goalId) {
        return contributions.findByGoalIdOrderByContributedOnDescIdDesc(goalId).stream()
            .map(GoalContribution::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Long, BigDecimal> savedByGoal(Long userId) {
        Map<Long, BigDecimal> saved = new HashMap<>();
        for (Object[] row : contributions.findAllGoalIdAndAmount(userId)) {
            saved.merge((Long) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        return saved;
    }

    private GoalResponse toResponse(Goal goal, BigDecimal savedRaw) {
        BigDecimal saved = Money.of(savedRaw);
        BigDecimal remaining = Money.of(goal.getTarget().subtract(saved).max(BigDecimal.ZERO));
        BigDecimal percent = saved.multiply(HUNDRED).divide(goal.getTarget(), 2, RoundingMode.HALF_EVEN);

        Integer monthsLeft = null;
        BigDecimal monthlyNeeded = null;
        if (goal.getDeadline() != null) {
            long months = ChronoUnit.MONTHS.between(YearMonth.now(clock), YearMonth.from(goal.getDeadline())) + 1;
            monthsLeft = (int) Math.max(0, months);
            monthlyNeeded = monthsLeft == 0 ? remaining
                : remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.CEILING);
        }
        return new GoalResponse(goal.getId(), goal.getName(), goal.getTarget(), goal.getDeadline(),
            goal.getPriority(), goal.isArchived(), saved, remaining, percent, monthsLeft, monthlyNeeded);
    }
}
