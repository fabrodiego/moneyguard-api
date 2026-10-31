package com.fabrodiego.moneyguard.insight;

/** Dimensoes temporais fixas dos insights. number: mes 1-12, trimestre 1-4, semestre 1-2, ano ignora. */
public enum InsightPeriod {
    MONTH(1),
    QUARTER(3),
    SEMESTER(6),
    YEAR(12);

    private final int months;

    InsightPeriod(int months) {
        this.months = months;
    }

    public int months() {
        return months;
    }

    public int maxNumber() {
        return 12 / months;
    }

    /** Numero do periodo que contem o mes informado (1-12). */
    public int numberOf(int month) {
        return (month - 1) / months + 1;
    }
}
