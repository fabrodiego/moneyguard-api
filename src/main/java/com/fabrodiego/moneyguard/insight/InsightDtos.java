package com.fabrodiego.moneyguard.insight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fabrodiego.moneyguard.transaction.TransactionType;

public final class InsightDtos {

    private InsightDtos() {
    }

    public record Totals(BigDecimal income, BigDecimal expense, BigDecimal balance) {
    }

    /** percent: participacao no total do mesmo tipo (gasto ou renda) no periodo. */
    public record CategoryTotal(Long categoryId, String name, TransactionType type, String color,
                                BigDecimal total, BigDecimal percent, int count) {
    }

    /** Fatia do grafico: dias (periodo MONTH) ou meses (demais periodos). */
    public record SeriesPoint(String label, LocalDate from, LocalDate to,
                              BigDecimal income, BigDecimal expense, BigDecimal balance) {
    }

    public record InsightResponse(
        InsightPeriod period,
        int year,
        int number,
        LocalDate from,
        LocalDate to,
        Totals totals,
        Totals previous,
        List<CategoryTotal> expensesByCategory,
        List<CategoryTotal> incomesByCategory,
        List<SeriesPoint> series) {
    }
}
