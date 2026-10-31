package com.fabrodiego.moneyguard.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fabrodiego.moneyguard.category.Category;
import com.fabrodiego.moneyguard.crypto.EncryptedBigDecimalConverter;
import com.fabrodiego.moneyguard.crypto.EncryptedStringConverter;
import com.fabrodiego.moneyguard.merchant.Merchant;

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

@Entity
@Table(name = "transactions")
public class Transaction {

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

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "amount_enc", nullable = false)
    private BigDecimal amount;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "description_enc")
    private String description;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionSource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Transaction() {
    }

    public Transaction(Long userId, Category category, Merchant merchant, BigDecimal amount,
                       String description, LocalDate occurredOn, TransactionSource source) {
        this.userId = userId;
        this.source = source;
        update(category, merchant, amount, description, occurredOn);
    }

    /** O tipo sempre acompanha a categoria (gasto ou renda). */
    public void update(Category category, Merchant merchant, BigDecimal amount, String description, LocalDate occurredOn) {
        this.category = category;
        this.type = category.getType();
        this.merchant = merchant;
        this.amount = amount;
        this.description = description;
        this.occurredOn = occurredOn;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    public TransactionSource getSource() {
        return source;
    }
}
