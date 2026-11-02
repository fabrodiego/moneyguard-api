package com.fabrodiego.moneyguard.recurrence;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fabrodiego.moneyguard.category.Category;
import com.fabrodiego.moneyguard.category.CategoryService;
import com.fabrodiego.moneyguard.common.BadRequestException;
import com.fabrodiego.moneyguard.common.Money;
import com.fabrodiego.moneyguard.common.NotFoundException;
import com.fabrodiego.moneyguard.merchant.Merchant;
import com.fabrodiego.moneyguard.merchant.MerchantService;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.RecurrenceRequest;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.RecurrenceResponse;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.RecurrenceSummary;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.Upcoming;
import com.fabrodiego.moneyguard.transaction.Transaction;
import com.fabrodiego.moneyguard.transaction.TransactionRepository;
import com.fabrodiego.moneyguard.transaction.TransactionType;

@Service
public class RecurrenceService {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceService.class);

    private final RecurrenceRepository recurrences;
    private final TransactionRepository transactions;
    private final CategoryService categories;
    private final MerchantService merchants;
    private final TransactionTemplate tx;
    private final Clock clock;

    public RecurrenceService(RecurrenceRepository recurrences, TransactionRepository transactions,
                             CategoryService categories, MerchantService merchants,
                             PlatformTransactionManager txManager, Clock clock) {
        this.recurrences = recurrences;
        this.transactions = transactions;
        this.categories = categories;
        this.merchants = merchants;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RecurrenceResponse> list(Long userId) {
        return recurrences.findByUserIdOrderByIdAsc(userId).stream()
            .sorted(Comparator.comparing(Recurrence::isActive).reversed()
                .thenComparing(r -> r.getNextDueOn() == null ? LocalDate.MAX : r.getNextDueOn()))
            .map(RecurrenceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RecurrenceResponse get(Long userId, Long id) {
        return RecurrenceResponse.from(find(userId, id));
    }

    /** Se a primeira cobranca for hoje, ja lanca na hora. */
    @Transactional
    public RecurrenceResponse create(Long userId, RecurrenceRequest req) {
        Recurrence r = new Recurrence(userId);
        apply(userId, r, req);
        recurrences.save(r);
        launchDue(r, LocalDate.now(clock));
        return RecurrenceResponse.from(r);
    }

    @Transactional
    public RecurrenceResponse update(Long userId, Long id, RecurrenceRequest req) {
        Recurrence r = find(userId, id);
        apply(userId, r, req);
        launchDue(r, LocalDate.now(clock));
        return RecurrenceResponse.from(r);
    }

    /** Os lancamentos ja feitos ficam (sem o vinculo). */
    @Transactional
    public void delete(Long userId, Long id) {
        recurrences.delete(find(userId, id));
    }

    @Transactional(readOnly = true)
    public RecurrenceSummary summary(Long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate limit = today.plusDays(30);
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal incomes = BigDecimal.ZERO;
        int active = 0;
        List<Upcoming> upcoming = new ArrayList<>();
        for (Recurrence r : recurrences.findByUserIdOrderByIdAsc(userId)) {
            if (!r.isActive() || r.getNextDueOn() == null) {
                continue;
            }
            active++;
            BigDecimal monthly = RecurrenceSchedule.monthlyEquivalent(r.getFrequency(), r.getAmount());
            if (r.getType() == TransactionType.EXPENSE) {
                expenses = expenses.add(monthly);
            } else {
                incomes = incomes.add(monthly);
            }
            for (LocalDate due = r.getNextDueOn(); !due.isAfter(limit);
                 due = RecurrenceSchedule.next(r.getFrequency(), r.getDayOfMonth(), due)) {
                if (r.getEndDate() != null && due.isAfter(r.getEndDate())) {
                    break;
                }
                upcoming.add(new Upcoming(r.getId(), r.getName(), r.getType(), r.getAmount(), due));
            }
        }
        upcoming.sort(Comparator.comparing(Upcoming::dueOn));
        return new RecurrenceSummary(Money.of(expenses), Money.of(incomes), Money.of(incomes.subtract(expenses)),
            active, upcoming);
    }

    /**
     * Lanca todas as cobrancas vencidas (inclusive as perdidas enquanto a API estava fora do ar).
     * Uma transacao por assinatura: erro em uma nao trava as outras.
     */
    public int launchAllDue() {
        LocalDate today = LocalDate.now(clock);
        int launched = 0;
        for (Long id : recurrences.findDueIds(today)) {
            try {
                Integer count = tx.execute(status -> recurrences.findWithRelationsById(id)
                    .map(r -> launchDue(r, today)).orElse(0));
                launched += count == null ? 0 : count;
            } catch (RuntimeException e) {
                log.error("Falha ao lancar assinatura {}", id, e);
            }
        }
        if (launched > 0) {
            log.info("{} lancamento(s) de assinatura criado(s)", launched);
        }
        return launched;
    }

    private int launchDue(Recurrence r, LocalDate today) {
        int count = 0;
        while (r.isActive() && r.getNextDueOn() != null && !r.getNextDueOn().isAfter(today)) {
            LocalDate due = r.getNextDueOn();
            if (!transactions.existsByRecurrenceIdAndOccurredOn(r.getId(), due)) {
                transactions.save(Transaction.fromRecurrence(r.getUserId(), r.getCategory(), r.getMerchant(),
                    r.getAmount(), r.getName(), due, r.getId()));
                count++;
            }
            r.advance();
        }
        return count;
    }

    private void apply(Long userId, Recurrence r, RecurrenceRequest req) {
        if (req.frequency() == Frequency.YEARLY && req.monthOfYear() == null) {
            throw new BadRequestException("Informe o mes da cobranca anual");
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate start = req.startDate() != null ? req.startDate() : today;
        if (req.endDate() != null && req.endDate().isBefore(start)) {
            throw new BadRequestException("A data final precisa ser depois do inicio");
        }
        Category category = categories.get(userId, req.categoryId());
        Merchant merchant = req.merchantId() == null ? null : merchants.get(userId, req.merchantId());
        boolean active = req.active() == null || req.active();
        // Nunca lanca retroativo: a primeira cobranca considerada e hoje ou depois.
        r.update(category, merchant, req.name().trim(), Money.of(req.amount()), req.frequency(), req.dayOfMonth(),
            req.monthOfYear(), start, req.endDate(), active, today);
    }

    private Recurrence find(Long userId, Long id) {
        return recurrences.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new NotFoundException("Assinatura nao encontrada"));
    }
}
