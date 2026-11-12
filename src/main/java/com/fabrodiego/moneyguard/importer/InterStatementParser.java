package com.fabrodiego.moneyguard.importer;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fabrodiego.moneyguard.common.TextNormalizer;
import com.fabrodiego.moneyguard.transaction.TransactionType;

/**
 * Extrato da conta corrente do Inter em CSV.
 * Cabecalho livre (conta, periodo, saldo) e depois a tabela:
 * {@code Data Lançamento;Histórico;Descrição;Valor;Saldo}, data dd/MM/yyyy e valor "-1.580,92".
 */
@Component
public class InterStatementParser {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public ParsedFile parse(byte[] content) {
        List<String> lines = decode(content).lines().toList();
        int header = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (TextNormalizer.normalize(lines.get(i)).startsWith("data lancamento;")) {
                header = i;
                break;
            }
        }
        if (header < 0) {
            throw new ImportFormatException("Arquivo nao reconhecido: esperado o extrato CSV do Inter");
        }

        List<ParsedFile.Entry> entries = new ArrayList<>();
        List<ParsedFile.Ignored> ignored = new ArrayList<>();
        for (String line : lines.subList(header + 1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] cols = line.split(";", -1);
            if (cols.length < 4) {
                throw new ImportFormatException("Linha do extrato com formato inesperado");
            }
            LocalDate date = date(cols[0]);
            String history = BrazilianFormats.collapse(cols[1]);
            String description = BrazilianFormats.collapse(cols[2]);
            if (description.isEmpty()) {
                description = history;
            }
            BigDecimal value = BrazilianFormats.money(cols[3]);
            if (value.signum() == 0) {
                continue;
            }
            if (isBillPayment(history, description)) {
                ignored.add(new ParsedFile.Ignored(date, description, value.abs(), "Pagamento de fatura"));
                continue;
            }
            TransactionType type = value.signum() < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
            entries.add(new ParsedFile.Entry(date, description, value.abs(), type, history, null, null));
        }
        return new ParsedFile(ImportKind.STATEMENT, entries, ignored, null, null);
    }

    /** As compras da fatura sao importadas uma a uma; o pagamento dela no extrato contaria em dobro. */
    static boolean isBillPayment(String history, String description) {
        String h = TextNormalizer.normalize(history);
        String d = TextNormalizer.normalize(description);
        return d.contains("pagamento fatura") || d.contains("pagamento de fatura")
            || (h.startsWith("pagamento") && d.contains("fatura"));
    }

    private static LocalDate date(String text) {
        try {
            return LocalDate.parse(text.strip(), DATE);
        } catch (DateTimeParseException e) {
            throw new ImportFormatException("Data invalida no extrato: " + text.strip());
        }
    }

    /** O Inter exporta em UTF-8; se vier em Latin-1 (Excel), aceita tambem. */
    private static String decode(byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content)).toString();
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            return new String(content, StandardCharsets.ISO_8859_1);
        }
    }
}
