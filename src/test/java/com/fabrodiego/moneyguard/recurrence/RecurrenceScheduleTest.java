package com.fabrodiego.moneyguard.recurrence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class RecurrenceScheduleTest {

    @Test
    void mensalNoMesmoMesOuNoProximo() {
        LocalDate today = LocalDate.of(2026, 11, 2);
        assertThat(RecurrenceSchedule.firstOnOrAfter(Frequency.MONTHLY, 10, null, today)).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(RecurrenceSchedule.firstOnOrAfter(Frequency.MONTHLY, 2, null, today)).isEqualTo(today);
        assertThat(RecurrenceSchedule.firstOnOrAfter(Frequency.MONTHLY, 1, null, today)).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void dia31ViraUltimoDiaDoMes() {
        assertThat(RecurrenceSchedule.firstOnOrAfter(Frequency.MONTHLY, 31, null, LocalDate.of(2027, 2, 1)))
            .isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(RecurrenceSchedule.next(Frequency.MONTHLY, 31, LocalDate.of(2027, 2, 28)))
            .isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(RecurrenceSchedule.next(Frequency.MONTHLY, 31, LocalDate.of(2027, 3, 31)))
            .isEqualTo(LocalDate.of(2027, 4, 30));
    }

    @Test
    void anual() {
        LocalDate today = LocalDate.of(2026, 11, 2);
        assertThat(RecurrenceSchedule.firstOnOrAfter(Frequency.YEARLY, 15, 3, today)).isEqualTo(LocalDate.of(2027, 3, 15));
        assertThat(RecurrenceSchedule.firstOnOrAfter(Frequency.YEARLY, 20, 11, today)).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(RecurrenceSchedule.next(Frequency.YEARLY, 29, LocalDate.of(2028, 2, 29))).isEqualTo(LocalDate.of(2029, 2, 28));
        assertThat(RecurrenceSchedule.next(Frequency.YEARLY, 29, LocalDate.of(2029, 2, 28))).isEqualTo(LocalDate.of(2030, 2, 28));
    }

    @Test
    void equivalenteMensal() {
        assertThat(RecurrenceSchedule.monthlyEquivalent(Frequency.YEARLY, new BigDecimal("120.00"))).isEqualByComparingTo("10.00");
        assertThat(RecurrenceSchedule.monthlyEquivalent(Frequency.MONTHLY, new BigDecimal("55.90"))).isEqualByComparingTo("55.90");
    }
}
