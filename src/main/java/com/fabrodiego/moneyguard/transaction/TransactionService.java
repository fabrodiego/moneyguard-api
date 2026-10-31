package com.fabrodiego.moneyguard.transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.category.Category;
import com.fabrodiego.moneyguard.category.CategoryService;
import com.fabrodiego.moneyguard.common.BadRequestException;
import com.fabrodiego.moneyguard.common.Money;
import com.fabrodiego.moneyguard.common.NotFoundException;
import com.fabrodiego.moneyguard.merchant.Merchant;
import com.fabrodiego.moneyguard.merchant.MerchantService;
import com.fabrodiego.moneyguard.transaction.TransactionDtos.TransactionRequest;
import com.fabrodiego.moneyguard.transaction.TransactionDtos.TransactionResponse;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final CategoryService categories;
    private final MerchantService merchants;
    private final Clock clock;

    public TransactionService(TransactionRepository transactions, CategoryService categories,
                              MerchantService merchants, Clock clock) {
        this.transactions = transactions;
        this.categories = categories;
        this.merchants = merchants;
        this.clock = clock;
    }

    /** Sem periodo informado, devolve o mes atual. */
    @Transactional(readOnly = true)
    public List<TransactionResponse> search(Long userId, LocalDate from, LocalDate to, Long categoryId, TransactionType type) {
        YearMonth current = YearMonth.now(clock);
        LocalDate start = from != null ? from : current.atDay(1);
        LocalDate end = to != null ? to : current.atEndOfMonth();
        if (end.isBefore(start)) {
            throw new BadRequestException("'to' precisa ser igual ou depois de 'from'");
        }
        return transactions.search(userId, start, end, categoryId, type).stream()
            .map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long userId, Long id) {
        return TransactionResponse.from(find(userId, id));
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest req, TransactionSource source) {
        Category category = categories.get(userId, req.categoryId());
        Transaction saved = transactions.save(new Transaction(userId, category, merchant(userId, req.merchantId()),
            Money.of(req.amount()), blankToNull(req.description()), dateOrToday(req.occurredOn()), source));
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionRequest req) {
        Transaction transaction = find(userId, id);
        transaction.update(categories.get(userId, req.categoryId()), merchant(userId, req.merchantId()),
            Money.of(req.amount()), blankToNull(req.description()), dateOrToday(req.occurredOn()));
        return TransactionResponse.from(transaction);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        transactions.delete(find(userId, id));
    }

    /** Saldo de todo o historico: rendas - gastos. */
    @Transactional(readOnly = true)
    public BigDecimal allTimeBalance(Long userId) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Object[] row : transactions.findAllTypeAndAmount(userId)) {
            BigDecimal amount = (BigDecimal) row[1];
            balance = row[0] == TransactionType.INCOME ? balance.add(amount) : balance.subtract(amount);
        }
        return Money.of(balance);
    }

    private Transaction find(Long userId, Long id) {
        return transactions.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new NotFoundException("Lancamento nao encontrado"));
    }

    private Merchant merchant(Long userId, Long merchantId) {
        return merchantId == null ? null : merchants.get(userId, merchantId);
    }

    private LocalDate dateOrToday(LocalDate date) {
        return date != null ? date : LocalDate.now(clock);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
