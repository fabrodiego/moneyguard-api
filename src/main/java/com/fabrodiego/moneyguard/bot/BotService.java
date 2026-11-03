package com.fabrodiego.moneyguard.bot;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabrodiego.moneyguard.bot.BotDtos.BotRecurrenceRequest;
import com.fabrodiego.moneyguard.bot.BotDtos.BotRecurrenceResponse;
import com.fabrodiego.moneyguard.bot.BotDtos.BotTransactionRequest;
import com.fabrodiego.moneyguard.bot.BotDtos.RecurrenceInfo;
import com.fabrodiego.moneyguard.bot.BotDtos.RecurrenceList;
import com.fabrodiego.moneyguard.bot.BotDtos.ParseResponse;
import com.fabrodiego.moneyguard.bot.BotDtos.Ref;
import com.fabrodiego.moneyguard.bot.BotDtos.Summary;
import com.fabrodiego.moneyguard.bot.MessageParser.CategoryRef;
import com.fabrodiego.moneyguard.bot.MessageParser.MerchantRef;
import com.fabrodiego.moneyguard.bot.MessageParser.Parsed;
import com.fabrodiego.moneyguard.category.Category;
import com.fabrodiego.moneyguard.category.CategoryRepository;
import com.fabrodiego.moneyguard.discord.DiscordLinkService;
import com.fabrodiego.moneyguard.goal.GoalService;
import com.fabrodiego.moneyguard.insight.InsightDtos.InsightResponse;
import com.fabrodiego.moneyguard.insight.InsightPeriod;
import com.fabrodiego.moneyguard.insight.InsightService;
import com.fabrodiego.moneyguard.merchant.MerchantRepository;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.RecurrenceRequest;
import com.fabrodiego.moneyguard.recurrence.RecurrenceDtos.RecurrenceResponse;
import com.fabrodiego.moneyguard.recurrence.RecurrenceService;
import com.fabrodiego.moneyguard.transaction.TransactionRepository;
import com.fabrodiego.moneyguard.transaction.TransactionDtos.TransactionRequest;
import com.fabrodiego.moneyguard.transaction.TransactionDtos.TransactionResponse;
import com.fabrodiego.moneyguard.transaction.TransactionService;
import com.fabrodiego.moneyguard.transaction.TransactionSource;
import com.fabrodiego.moneyguard.user.UserRepository;

/** Tudo que o bot faz, sempre em nome do usuario vinculado ao discordUserId. */
@Service
public class BotService {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final DiscordLinkService links;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final MerchantRepository merchants;
    private final TransactionService transactions;
    private final InsightService insights;
    private final GoalService goals;
    private final RecurrenceService recurrences;
    private final TransactionRepository transactionsRepo;
    private final Clock clock;

    public BotService(DiscordLinkService links, UserRepository users, CategoryRepository categories,
                      MerchantRepository merchants, TransactionService transactions, InsightService insights,
                      GoalService goals, RecurrenceService recurrences, TransactionRepository transactionsRepo,
                      Clock clock) {
        this.links = links;
        this.users = users;
        this.categories = categories;
        this.merchants = merchants;
        this.transactions = transactions;
        this.insights = insights;
        this.goals = goals;
        this.recurrences = recurrences;
        this.transactionsRepo = transactionsRepo;
        this.clock = clock;
    }

    @Transactional
    public String link(String discordUserId, String code) {
        Long userId = links.link(discordUserId, code);
        return users.findById(userId).map(u -> u.getName()).orElse("");
    }

    @Transactional
    public void unlink(String discordUserId) {
        links.unlink(links.userIdOf(discordUserId));
    }

    @Transactional(readOnly = true)
    public ParseResponse parse(String discordUserId, String text) {
        Long userId = links.userIdOf(discordUserId);
        List<CategoryRef> cats = categories.findByUserIdOrderByTypeAscNameAsc(userId).stream()
            .map(BotService::ref).toList();
        List<MerchantRef> merch = merchants.findByUserIdOrderByNameAsc(userId).stream()
            .map(m -> new MerchantRef(m.getId(), m.getName(), ref(m.getCategory()))).toList();

        Parsed p = MessageParser.parse(text, cats, merch, LocalDate.now(clock));
        RecurrenceInfo recurrence = p.recurrence() == null ? null
            : new RecurrenceInfo(p.recurrence().frequency(), p.recurrence().dayOfMonth(), p.recurrence().monthOfYear());
        return new ParseResponse(p.amount(), p.type(), p.occurredOn(), p.description(),
            p.category() == null ? null : new Ref(p.category().id(), p.category().name()),
            p.merchant() == null ? null : new Ref(p.merchant().id(), p.merchant().name()), recurrence);
    }

    @Transactional(readOnly = true)
    public Long userId(String discordUserId) {
        return links.userIdOf(discordUserId);
    }

    @Transactional
    public TransactionResponse createTransaction(BotTransactionRequest req) {
        Long userId = links.userIdOf(req.discordUserId());
        return transactions.create(userId, new TransactionRequest(req.categoryId(), req.merchantId(), req.amount(),
            req.description(), req.occurredOn()), TransactionSource.DISCORD);
    }

    @Transactional
    public BotRecurrenceResponse createRecurrence(BotRecurrenceRequest req) {
        Long userId = links.userIdOf(req.discordUserId());
        RecurrenceResponse created = recurrences.create(userId, new RecurrenceRequest(req.name(), req.categoryId(),
            req.merchantId(), req.amount(), req.frequency(), req.dayOfMonth(), req.monthOfYear(), null, null, true));
        LocalDate today = LocalDate.now(clock);
        boolean launchedToday = created.nextDueOn() != null && created.nextDueOn().isAfter(today)
            && transactionsRepo.existsByRecurrenceIdAndOccurredOn(created.id(), today);
        return new BotRecurrenceResponse(created, launchedToday);
    }

    @Transactional(readOnly = true)
    public RecurrenceList recurrences(String discordUserId) {
        Long userId = links.userIdOf(discordUserId);
        return new RecurrenceList(recurrences.summary(userId),
            recurrences.list(userId).stream().filter(r -> r.active() && r.nextDueOn() != null).toList());
    }

    @Transactional
    public void deleteTransaction(String discordUserId, Long transactionId) {
        transactions.delete(links.userIdOf(discordUserId), transactionId);
    }

    @Transactional(readOnly = true)
    public Summary summary(String discordUserId) {
        Long userId = links.userIdOf(discordUserId);
        YearMonth now = YearMonth.now(clock);
        InsightResponse month = insights.get(userId, InsightPeriod.MONTH, now.getYear(), now.getMonthValue());
        String label = now.getMonth().getDisplayName(TextStyle.FULL, PT_BR) + " de " + now.getYear();
        String name = users.findById(userId).map(u -> u.getName()).orElse("");
        return new Summary(name, Character.toUpperCase(label.charAt(0)) + label.substring(1), month.totals(),
            month.expensesByCategory().stream().limit(3).toList(), goals.summary(userId));
    }

    private static CategoryRef ref(Category c) {
        return new CategoryRef(c.getId(), c.getName(), c.getType());
    }
}
