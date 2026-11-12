package com.fabrodiego.moneyguard.importer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import com.fabrodiego.moneyguard.common.Money;
import com.fabrodiego.moneyguard.common.TextNormalizer;
import com.fabrodiego.moneyguard.transaction.TransactionType;

/**
 * Fatura do cartao Inter em PDF (texto selecionavel). As compras ficam em "Despesas da fatura",
 * uma tabela por cartao (fisico e virtuais):
 * {@code 23 de set. 2026  LOJA X  (Parcela 02 de 02)  -  R$ 59,90}. Credito vem com "+ R$".
 * A secao "Proxima fatura" (parcelas futuras) nao entra.
 */
@Component
public class InterCardBillParser {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private static final Pattern LINE = Pattern.compile(
        "^(\\d{1,2}) de ([a-z]{3,4})\\.? (\\d{4}) (.+?) (\\+ ?)?R\\$ ?(\\d{1,3}(?:\\.\\d{3})*,\\d{2})$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern INSTALLMENT = Pattern.compile("\\(\\s*parcela (\\d{1,2}) de (\\d{1,2})\\s*\\)",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern DECLARED_TOTAL = Pattern.compile(
        "despesas do mes[ \\t]*r\\$[ \\t]*(\\d{1,3}(?:\\.\\d{3})*,\\d{2})");

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
        Map.entry("jan", 1), Map.entry("fev", 2), Map.entry("mar", 3), Map.entry("abr", 4),
        Map.entry("mai", 5), Map.entry("jun", 6), Map.entry("jul", 7), Map.entry("ago", 8),
        Map.entry("set", 9), Map.entry("out", 10), Map.entry("nov", 11), Map.entry("dez", 12));

    public ParsedFile parse(byte[] content) {
        return parseText(extractText(content));
    }

    ParsedFile parseText(String text) {
        if (!TextNormalizer.normalize(text).contains("despesas da fatura")) {
            throw new ImportFormatException("Arquivo nao reconhecido: esperada a fatura em PDF do cartao Inter");
        }
        List<ParsedFile.Entry> entries = new ArrayList<>();
        List<ParsedFile.Ignored> ignored = new ArrayList<>();
        BigDecimal parsedTotal = BigDecimal.ZERO;

        for (String raw : text.lines().toList()) {
            String line = BrazilianFormats.collapse(raw);
            String normalized = TextNormalizer.normalize(line);
            if (normalized.startsWith("proxima fatura")) {
                break;
            }
            Matcher m = LINE.matcher(line);
            if (!m.matches()) {
                continue;
            }
            LocalDate date = date(m.group(1), m.group(2), m.group(3));
            if (date == null) {
                continue;
            }
            String body = m.group(4);
            Integer installment = null;
            Integer installments = null;
            Matcher inst = INSTALLMENT.matcher(body);
            if (inst.find()) {
                installment = Integer.valueOf(inst.group(1));
                installments = Integer.valueOf(inst.group(2));
                body = body.substring(0, inst.start()) + " " + body.substring(inst.end());
            }
            String description = cleanDescription(body);
            BigDecimal amount = BrazilianFormats.money(m.group(6));
            boolean credit = m.group(5) != null;

            if (credit) {
                boolean payment = TextNormalizer.normalize(description).replace(" ", "").contains("pagamento");
                ignored.add(new ParsedFile.Ignored(date, description, amount,
                    payment ? "Pagamento da fatura" : "Estorno / credito"));
                if (!payment) {
                    parsedTotal = parsedTotal.subtract(amount);
                }
                continue;
            }
            // parcela N aparece com a data da compra; lanca no mes em que foi cobrada
            LocalDate occurredOn = installment == null ? date : date.plusMonths(installment - 1L);
            entries.add(new ParsedFile.Entry(occurredOn, description, amount, TransactionType.EXPENSE,
                null, installment, installments));
            parsedTotal = parsedTotal.add(amount);
        }
        if (entries.isEmpty() && ignored.isEmpty()) {
            throw new ImportFormatException("Nenhuma compra encontrada na fatura");
        }
        return new ParsedFile(ImportKind.CARD_BILL, entries, ignored, declaredTotal(text), Money.of(parsedTotal));
    }

    private static BigDecimal declaredTotal(String text) {
        Matcher m = DECLARED_TOTAL.matcher(TextNormalizer.normalize(text));
        return m.find() ? BrazilianFormats.money(m.group(1)) : null;
    }

    /** Tira a coluna "Beneficiario" vazia ("-") que sobra no fim. */
    private static String cleanDescription(String body) {
        String text = BrazilianFormats.collapse(body);
        while (text.endsWith(" -") || text.equals("-")) {
            text = text.substring(0, text.length() - 1).strip();
        }
        return text;
    }

    private static LocalDate date(String day, String month, String year) {
        Integer m = MONTHS.get(month.substring(0, 3).toLowerCase(java.util.Locale.ROOT));
        if (m == null) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(year), m, Integer.parseInt(day));
        } catch (DateTimeException e) {
            return null;
        }
    }

    /** Aceita lixo antes do cabecalho %PDF (ja chegou arquivo com bytes zerados no inicio). */
    private static String extractText(byte[] content) {
        int start = indexOf(content, PDF_MAGIC);
        if (start < 0) {
            throw new ImportFormatException("Arquivo nao e um PDF");
        }
        byte[] pdf = start == 0 ? content : Arrays.copyOfRange(content, start, content.length);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        } catch (IOException e) {
            throw new ImportFormatException("Nao foi possivel ler o PDF (protegido por senha ou corrompido)");
        }
    }

    static int indexOf(byte[] data, byte[] pattern) {
        int limit = Math.min(data.length - pattern.length, 4 * 1024 * 1024);
        outer:
        for (int i = 0; i <= limit; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
