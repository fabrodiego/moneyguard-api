package com.fabrodiego.moneyguard.importer;

public enum ImportKind {
    /** Extrato da conta corrente (CSV). */
    STATEMENT,
    /** Fatura do cartao de credito (PDF). */
    CARD_BILL
}
