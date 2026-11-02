package com.fabrodiego.moneyguard.recurrence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fabrodiego.moneyguard.category.Category;
import com.fabrodiego.moneyguard.crypto.EncryptedBigDecimalConverter;
import com.fabrodiego.moneyguard.crypto.EncryptedStringConverter;
import com.fabrodiego.moneyguard.merchant.Merchant;
import com.fabrodiego.moneyguard.transaction.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Assinatura / conta fixa / renda fixa: vira um lancamento automatico a cada cobranca. */
@Entity
@Table(name = "recurrences")
public class Recurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "name_enc", nullable = false)
    private String name;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "amount_enc", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;

    @Column(name = "month_of_year")
    private Integer monthOfYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** Proxima cobranca ainda nao lancada. null = encerrada. */
    @Column(name = "next_due_on")
    private LocalDate nextDueOn;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Recurrence() {
    }

    public Recurrence(Long userId) {
        this.userId = userId;
    }

    /**
     * Atualiza os dados e recalcula a proxima cobranca a partir de {@code from}
     * (lancamentos ja feitos nao sao refeitos).
     */
    public void update(Category category, Merchant merchant, String name, BigDecimal amount, Frequency frequency,
                       int dayOfMonth, Integer monthOfYear, LocalDate startDate, LocalDate endDate, boolean active,
                       LocalDate from) {
        this.category = category;
        this.type = category.getType();
        this.merchant = merchant;
        this.name = name;
        this.amount = amount;
        this.frequency = frequency;
        this.dayOfMonth = dayOfMonth;
        this.monthOfYear = frequency == Frequency.YEARLY ? monthOfYear : null;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
        LocalDate base = startDate.isAfter(from) ? startDate : from;
        this.nextDueOn = withinEnd(RecurrenceSchedule.firstOnOrAfter(frequency, dayOfMonth, this.monthOfYear, base));
    }

    /** Depois de lancar a cobranca de nextDueOn, avanca para a seguinte. */
    public void advance() {
        this.nextDueOn = withinEnd(RecurrenceSchedule.next(frequency, dayOfMonth, nextDueOn));
    }

    private LocalDate withinEnd(LocalDate date) {
        return endDate != null && date.isAfter(endDate) ? null : date;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Category getCategory() {
        return category;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public TransactionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public Integer getMonthOfYear() {
        return monthOfYear;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getNextDueOn() {
        return nextDueOn;
    }

    public boolean isActive() {
        return active;
    }
}
