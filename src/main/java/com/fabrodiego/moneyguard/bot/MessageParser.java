package com.fabrodiego.moneyguard.bot;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fabrodiego.moneyguard.common.Money;
import com.fabrodiego.moneyguard.transaction.TransactionType;

/**
 * Interpreta frases livres do Discord, por regras (sem IA externa):
 * "acabei de gastar 8 reais na sodexo", "recebi 6.500 de salario", "paguei R$ 120,50 de luz ontem".
 *
 * Trabalha sobre um texto "dobrado" (minusculo, sem acento) com o MESMO tamanho do original,
 * para recortar a descricao do texto original sem perder os acentos.
 */
public final class MessageParser {

    public record CategoryRef(Long id, String name, TransactionType type) {
    }

    public record MerchantRef(Long id, String name, CategoryRef category) {
    }

    public record Parsed(
        BigDecimal amount,
        TransactionType type,
        CategoryRef category,
        MerchantRef merchant,
        LocalDate occurredOn,
        String description) {
    }

    private static final Set<String> INCOME_WORDS = Set.of(
        "recebi", "receber", "recebido", "ganhei", "ganho", "entrou", "caiu", "salario", "rendeu",
        "rendimento", "rendimentos", "vendi", "reembolso", "freela", "renda");
    private static final Set<String> EXPENSE_WORDS = Set.of(
        "gastei", "gastar", "gasto", "paguei", "pagar", "pago", "comprei", "comprar", "custou", "saiu", "torrei");
    private static final Set<String> PREPOSITIONS = Set.of(
        "em", "no", "na", "nos", "nas", "com", "de", "do", "da", "dos", "das", "pra", "para", "pro", "num", "numa");
    private static final Set<String> FILLER = Set.of(
        "hoje", "ontem", "anteontem", "agora", "reais", "real", "conto", "contos", "pila", "pilas", "r",
        "acabei", "de", "gastar", "gastei", "paguei", "comprei", "recebi", "ganhei", "um", "uma", "o", "a", "e");

    private static final Pattern RELATIVE_DAY = Pattern.compile("\\b(anteontem|ontem|hoje)\\b");
    private static final Pattern DAY_MONTH = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b");
    private static final Pattern DAY_ONLY = Pattern.compile("\\bdia (\\d{1,2})\\b");
    private static final Pattern AMOUNT = Pattern.compile(
        "(r\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)(\\s*mil\\b)?"
            + "(\\s*(?:reais|real|conto|contos|pila|pilas)\\b)?");

    private MessageParser() {
    }

    public static Parsed parse(String text, List<CategoryRef> categories, List<MerchantRef> merchants, LocalDate today) {
        String original = text == null ? "" : text;
        StringBuilder work = new StringBuilder(fold(original));

        LocalDate date = extractDate(work, today);
        BigDecimal amount = extractAmount(work);
        String folded = work.toString();

        Optional<MerchantRef> merchant = merchants.stream()
            .filter(m -> containsWord(folded, fold(m.name())))
            .max(Comparator.comparingInt(m -> m.name().length()));

        TransactionType type = merchant.map(m -> m.category().type()).orElseGet(() -> detectType(folded));

        Optional<CategoryRef> category = merchant.map(MerchantRef::category).or(() -> categories.stream()
            .filter(c -> c.type() == type && containsWord(folded, fold(c.name())))
            .max(Comparator.comparingInt(c -> c.name().length())));

        String description = merchant.isPresent() ? null : describe(original, work.toString(), category.orElse(null));
        return new Parsed(amount, type, category.orElse(null), merchant.orElse(null), date, description);
    }

    /** Minusculo e sem acento, caractere por caractere (mesmo tamanho do original). */
    static String fold(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            String base = Normalizer.normalize(String.valueOf(ch), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            String lower = (base.length() == 1 ? base : String.valueOf(ch)).toLowerCase(Locale.ROOT);
            out.append(lower.length() == 1 ? lower.charAt(0) : ch);
        }
        return out.toString();
    }

    private static LocalDate extractDate(StringBuilder work, LocalDate today) {
        LocalDate date = today;
        Matcher m = DAY_MONTH.matcher(work);
        if (m.find()) {
            int day = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int year = m.group(3) == null ? today.getYear() : normalizeYear(Integer.parseInt(m.group(3)));
            LocalDate candidate = safeDate(year, month, day);
            if (candidate != null) {
                if (m.group(3) == null && candidate.isAfter(today)) {
                    candidate = candidate.minusYears(1);
                }
                blank(work, m.start(), m.end());
                return candidate;
            }
        }
        m = DAY_ONLY.matcher(work);
        if (m.find()) {
            int day = Integer.parseInt(m.group(1));
            LocalDate candidate = safeDate(today.getYear(), today.getMonthValue(), day);
            if (candidate != null) {
                if (candidate.isAfter(today)) {
                    LocalDate prev = today.minusMonths(1);
                    candidate = safeDate(prev.getYear(), prev.getMonthValue(), day);
                }
                if (candidate != null) {
                    blank(work, m.start(), m.end());
                    return candidate;
                }
            }
        }
        m = RELATIVE_DAY.matcher(work);
        if (m.find()) {
            date = switch (m.group(1)) {
                case "anteontem" -> today.minusDays(2);
                case "ontem" -> today.minusDays(1);
                default -> today;
            };
            blank(work, m.start(), m.end());
        }
        return date;
    }

    /** Prefere o numero marcado como dinheiro (R$ antes ou "reais" depois); senao, o primeiro. */
    private static BigDecimal extractAmount(StringBuilder work) {
        Matcher m = AMOUNT.matcher(work);
        int[] chosen = null;
        BigDecimal value = null;
        while (m.find()) {
            boolean marked = m.group(1) != null || m.group(4) != null;
            if (chosen == null || marked) {
                value = toNumber(m.group(2));
                if (m.group(3) != null && value != null) {
                    value = value.multiply(BigDecimal.valueOf(1000));
                }
                chosen = new int[] {m.start(), m.end()};
                if (marked) {
                    break;
                }
            }
        }
        if (chosen != null) {
            blank(work, chosen[0], chosen[1]);
        }
        return value == null || value.signum() <= 0 ? null : Money.of(value);
    }

    private static BigDecimal toNumber(String raw) {
        String s = raw;
        if (s.contains(",")) {
            s = s.replace(".", "").replace(',', '.');
        } else if (s.matches("\\d{1,3}(\\.\\d{3})+")) {
            s = s.replace(".", "");
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static TransactionType detectType(String folded) {
        boolean income = words(folded).anyMatch(INCOME_WORDS::contains);
        boolean expense = words(folded).anyMatch(EXPENSE_WORDS::contains);
        return income && !expense ? TransactionType.INCOME : TransactionType.EXPENSE;
    }

    /**
     * Descricao = o que vem depois da ultima preposicao ("gastei 30 com uber" -> "uber"),
     * recortado do texto original. Nao repete o nome da categoria.
     */
    private static String describe(String original, String folded, CategoryRef category) {
        String[] tokens = folded.split("(?<=\\s)|(?=\\s)");
        int pos = 0;
        int lastPrepEnd = -1;
        for (String token : tokens) {
            String w = token.trim().replaceAll("[^\\p{L}\\p{N}]", "");
            if (PREPOSITIONS.contains(w)) {
                lastPrepEnd = pos + token.length();
            }
            pos += token.length();
        }
        if (lastPrepEnd < 0 || lastPrepEnd >= original.length()) {
            return null;
        }
        String tailFolded = folded.substring(lastPrepEnd);
        String tail = original.substring(lastPrepEnd);
        StringBuilder kept = new StringBuilder();
        String[] foldedWords = tailFolded.trim().split("\\s+");
        String[] originalWords = tail.trim().split("\\s+");
        for (int i = 0; i < foldedWords.length && i < originalWords.length; i++) {
            String w = foldedWords[i].replaceAll("[^\\p{L}\\p{N}]", "");
            if (w.isEmpty() || FILLER.contains(w)) {
                continue;
            }
            if (!kept.isEmpty()) {
                kept.append(' ');
            }
            kept.append(originalWords[i].replaceAll("[.!?,;:]+$", ""));
        }
        String result = kept.toString().trim();
        if (result.isEmpty()) {
            return null;
        }
        if (category != null && fold(result).equals(fold(category.name()))) {
            return null;
        }
        result = result.length() > 60 ? result.substring(0, 60).trim() : result;
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    private static boolean containsWord(String haystack, String needle) {
        String n = needle.trim().replaceAll("\\s+", " ");
        if (n.isEmpty()) {
            return false;
        }
        return Pattern.compile("(^|[^\\p{L}\\p{N}])" + Pattern.quote(n) + "($|[^\\p{L}\\p{N}])").matcher(haystack).find();
    }

    private static java.util.stream.Stream<String> words(String s) {
        return java.util.Arrays.stream(s.split("[^\\p{L}\\p{N}]+")).filter(w -> !w.isEmpty());
    }

    private static void blank(StringBuilder sb, int start, int end) {
        for (int i = start; i < end; i++) {
            sb.setCharAt(i, ' ');
        }
    }

    private static int normalizeYear(int year) {
        return year < 100 ? 2000 + year : year;
    }

    private static LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (java.time.DateTimeException e) {
            return null;
        }
    }
}
