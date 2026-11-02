package com.fabrodiego.moneyguard.recurrence;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Calculo das datas de cobranca. Dia que nao existe no mes (31 em abril, 29/02 fora de bissexto)
 * vira o ultimo dia daquele mes.
 */
public final class RecurrenceSchedule {

    private RecurrenceSchedule() {
    }

    /** Primeira cobranca no dia {@code from} ou depois dele. */
    public static LocalDate firstOnOrAfter(Frequency frequency, int dayOfMonth, Integer monthOfYear, LocalDate from) {
        if (frequency == Frequency.MONTHLY) {
            LocalDate candidate = clamp(YearMonth.from(from), dayOfMonth);
            return candidate.isBefore(from) ? clamp(YearMonth.from(from).plusMonths(1), dayOfMonth) : candidate;
        }
        LocalDate candidate = clamp(YearMonth.of(from.getYear(), monthOfYear), dayOfMonth);
        return candidate.isBefore(from) ? clamp(YearMonth.of(from.getYear() + 1, monthOfYear), dayOfMonth) : candidate;
    }

    /** Cobranca seguinte a {@code current}. */
    public static LocalDate next(Frequency frequency, int dayOfMonth, LocalDate current) {
        YearMonth ym = YearMonth.from(current);
        return frequency == Frequency.MONTHLY ? clamp(ym.plusMonths(1), dayOfMonth) : clamp(ym.plusYears(1), dayOfMonth);
    }

    /** Valor equivalente por mes (anual / 12). */
    public static java.math.BigDecimal monthlyEquivalent(Frequency frequency, java.math.BigDecimal amount) {
        return frequency == Frequency.MONTHLY ? amount
            : amount.divide(java.math.BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_EVEN);
    }

    private static LocalDate clamp(YearMonth ym, int day) {
        return ym.atDay(Math.min(day, ym.lengthOfMonth()));
    }
}
