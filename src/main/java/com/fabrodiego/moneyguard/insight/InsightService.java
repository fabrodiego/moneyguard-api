package com.fabrodiego.moneyguard.insight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.category.Category;
import com.fabrodiego.moneyguard.common.BadRequestException;
import com.fabrodiego.moneyguard.common.Money;
import com.fabrodiego.moneyguard.insight.InsightDtos.CategoryTotal;
import com.fabrodiego.moneyguard.insight.InsightDtos.InsightResponse;
import com.fabrodiego.moneyguard.insight.InsightDtos.SeriesPoint;
import com.fabrodiego.moneyguard.insight.InsightDtos.Totals;
import com.fabrodiego.moneyguard.transaction.Transaction;
import com.fabrodiego.moneyguard.transaction.TransactionRepository;
import com.fabrodiego.moneyguard.transaction.TransactionType;

/** Os valores sao cifrados no banco, entao toda agregacao acontece aqui, em memoria. */
@Service
public class InsightService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TransactionRepository transactions;
    private final Clock clock;

    public InsightService(TransactionRepository transactions, Clock clock) {
        this.transactions = transactions;
        this.clock = clock;
    }

    /** year/number nulos = periodo atual. */
    @Transactional(readOnly = true)
    public InsightResponse get(Long userId, InsightPeriod period, Integer year, Integer number) {
        YearMonth now = YearMonth.now(clock);
        int y = year != null ? year : now.getYear();
        int n = period == InsightPeriod.YEAR ? 1 : number != null ? number : period.numberOf(now.getMonthValue());
        if (n < 1 || n > period.maxNumber()) {
            throw new BadRequestException("number deve estar entre 1 e " + period.maxNumber() + " para " + period);
        }

        YearMonth firstMonth = YearMonth.of(y, (n - 1) * period.months() + 1);
        YearMonth lastMonth = firstMonth.plusMonths(period.months() - 1L);
        LocalDate from = firstMonth.atDay(1);
        LocalDate to = lastMonth.atEndOfMonth();

        List<Transaction> current = transactions.search(userId, from, to, null, null);
        YearMonth prevFirst = firstMonth.minusMonths(period.months());
        List<Transaction> previous = transactions.search(userId, prevFirst.atDay(1),
            prevFirst.plusMonths(period.months() - 1L).atEndOfMonth(), null, null);

        return new InsightResponse(period, y, n, from, to,
            totals(current), totals(previous),
            byCategory(current, TransactionType.EXPENSE),
            byCategory(current, TransactionType.INCOME),
            series(current, period, firstMonth, lastMonth));
    }

    private static Totals totals(List<Transaction> list) {
        BigDecimal income = sum(list, TransactionType.INCOME);
        BigDecimal expense = sum(list, TransactionType.EXPENSE);
        return new Totals(income, expense, Money.of(income.subtract(expense)));
    }

    private static BigDecimal sum(List<Transaction> list, TransactionType type) {
        return Money.of(list.stream().filter(t -> t.getType() == type)
            .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static List<CategoryTotal> byCategory(List<Transaction> list, TransactionType type) {
        Map<Long, List<Transaction>> grouped = new LinkedHashMap<>();
        list.stream().filter(t -> t.getType() == type)
            .forEach(t -> grouped.computeIfAbsent(t.getCategory().getId(), k -> new ArrayList<>()).add(t));
        BigDecimal total = sum(list, type);

        return grouped.values().stream().map(items -> {
            Category c = items.getFirst().getCategory();
            BigDecimal categoryTotal = sum(items, type);
            BigDecimal percent = total.signum() == 0 ? BigDecimal.ZERO
                : categoryTotal.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_EVEN);
            return new CategoryTotal(c.getId(), c.getName(), c.getType(), c.getColor(), categoryTotal, percent, items.size());
        }).sorted(Comparator.comparing(CategoryTotal::total).reversed()).toList();
    }

    private static List<SeriesPoint> series(List<Transaction> list, InsightPeriod period,
                                            YearMonth firstMonth, YearMonth lastMonth) {
        List<SeriesPoint> points = new ArrayList<>();
        if (period == InsightPeriod.MONTH) {
            for (int day = 1; day <= firstMonth.lengthOfMonth(); day++) {
                LocalDate date = firstMonth.atDay(day);
                points.add(point(String.format("%02d", day), date, date, list));
            }
        } else {
            for (YearMonth m = firstMonth; !m.isAfter(lastMonth); m = m.plusMonths(1)) {
                points.add(point(m.toString(), m.atDay(1), m.atEndOfMonth(), list));
            }
        }
        return points;
    }

    private static SeriesPoint point(String label, LocalDate from, LocalDate to, List<Transaction> list) {
        List<Transaction> slice = list.stream()
            .filter(t -> !t.getOccurredOn().isBefore(from) && !t.getOccurredOn().isAfter(to)).toList();
        Totals t = totals(slice);
        return new SeriesPoint(label, from, to, t.income(), t.expense(), t.balance());
    }
}
