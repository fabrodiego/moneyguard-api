package com.fabrodiego.moneyguard.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fabrodiego.moneyguard.transaction.TransactionType;

/** Resultado da leitura de um arquivo do banco, antes de qualquer decisao do usuario. */
public record ParsedFile(ImportKind kind, List<Entry> entries, List<Ignored> ignored,
                         BigDecimal declaredTotal, BigDecimal parsedTotal) {

    /**
     * Uma linha aproveitavel. amount sempre positivo; o sinal vira o tipo.
     * history = coluna "Historico" do extrato (null na fatura).
     * installment/installments = parcela N de M (null quando a vista).
     */
    public record Entry(LocalDate date, String description, BigDecimal amount, TransactionType type,
                        String history, Integer installment, Integer installments) {
    }

    /** Linha descartada automaticamente (pagamento de fatura, estorno). */
    public record Ignored(LocalDate date, String description, BigDecimal amount, String reason) {
    }
}
