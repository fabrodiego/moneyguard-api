package com.fabrodiego.moneyguard.recurrence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fabrodiego.moneyguard.category.CategoryDtos.CategoryResponse;
import com.fabrodiego.moneyguard.transaction.TransactionDtos.MerchantRef;
import com.fabrodiego.moneyguard.transaction.TransactionType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class RecurrenceDtos {

    private RecurrenceDtos() {
    }

    /** startDate vazio = hoje. monthOfYear obrigatorio quando YEARLY. */
    public record RecurrenceRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull Long categoryId,
        Long merchantId,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull Frequency frequency,
        @NotNull @Min(1) @Max(31) Integer dayOfMonth,
        @Min(1) @Max(12) Integer monthOfYear,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active) {
    }

    public record RecurrenceResponse(
        Long id,
        String name,
        TransactionType type,
        BigDecimal amount,
        BigDecimal monthlyAmount,
        Frequency frequency,
        int dayOfMonth,
        Integer monthOfYear,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextDueOn,
        boolean active,
        CategoryResponse category,
        MerchantRef merchant) {

        public static RecurrenceResponse from(Recurrence r) {
            MerchantRef merchant = r.getMerchant() == null ? null
                : new MerchantRef(r.getMerchant().getId(), r.getMerchant().getName());
            return new RecurrenceResponse(r.getId(), r.getName(), r.getType(), r.getAmount(),
                RecurrenceSchedule.monthlyEquivalent(r.getFrequency(), r.getAmount()), r.getFrequency(),
                r.getDayOfMonth(), r.getMonthOfYear(), r.getStartDate(), r.getEndDate(), r.getNextDueOn(),
                r.isActive(), CategoryResponse.from(r.getCategory()), merchant);
        }
    }

    public record Upcoming(Long recurrenceId, String name, TransactionType type, BigDecimal amount, LocalDate dueOn) {
    }

    /** Totais por mes so das assinaturas ativas (anuais entram como valor/12). */
    public record RecurrenceSummary(
        BigDecimal monthlyExpenses,
        BigDecimal monthlyIncomes,
        BigDecimal monthlyBalance,
        int activeCount,
        List<Upcoming> next30Days) {
    }
}
