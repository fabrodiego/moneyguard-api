package com.fabrodiego.moneyguard.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fabrodiego.moneyguard.crypto.EncryptedBigDecimalConverter;
import com.fabrodiego.moneyguard.crypto.EncryptedStringConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Aporte manual numa meta. Valor negativo = retirada. */
@Entity
@Table(name = "goal_contributions")
public class GoalContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", updatable = false)
    private Goal goal;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "amount_enc", nullable = false)
    private BigDecimal amount;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "note_enc")
    private String note;

    @Column(name = "contributed_on", nullable = false)
    private LocalDate contributedOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected GoalContribution() {
    }

    public GoalContribution(Goal goal, BigDecimal amount, String note, LocalDate contributedOn) {
        this.goal = goal;
        this.amount = amount;
        this.note = note;
        this.contributedOn = contributedOn;
    }

    public Long getId() {
        return id;
    }

    public Goal getGoal() {
        return goal;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getContributedOn() {
        return contributedOn;
    }
}
