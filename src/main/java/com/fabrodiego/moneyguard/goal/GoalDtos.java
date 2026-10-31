package com.fabrodiego.moneyguard.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class GoalDtos {

    private GoalDtos() {
    }

    public record GoalRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal target,
        LocalDate deadline,
        @Min(1) @Max(100) Integer priority,
        Boolean archived) {
    }

    /** amount negativo = retirada. contributedOn vazio = hoje. */
    public record ContributionRequest(
        @NotNull @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 255) String note,
        LocalDate contributedOn) {
    }

    public record ContributionResponse(Long id, BigDecimal amount, String note, LocalDate contributedOn) {

        public static ContributionResponse from(GoalContribution c) {
            return new ContributionResponse(c.getId(), c.getAmount(), c.getNote(), c.getContributedOn());
        }
    }

    /**
     * percent: 0-100 (pode passar de 100). monthsLeft conta o mes atual.
     * monthlyNeeded: quanto guardar por mes para bater o prazo (null sem prazo).
     */
    public record GoalResponse(
        Long id,
        String name,
        BigDecimal target,
        LocalDate deadline,
        int priority,
        boolean archived,
        BigDecimal saved,
        BigDecimal remaining,
        BigDecimal percent,
        Integer monthsLeft,
        BigDecimal monthlyNeeded) {
    }

    /** balance = rendas - gastos de todo o historico; available = balance - total ja aportado. */
    public record GoalSummary(
        BigDecimal balance,
        BigDecimal allocated,
        BigDecimal available,
        List<GoalResponse> goals) {
    }
}
