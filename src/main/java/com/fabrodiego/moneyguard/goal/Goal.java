package com.fabrodiego.moneyguard.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fabrodiego.moneyguard.crypto.EncryptedBigDecimalConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "target_enc", nullable = false)
    private BigDecimal target;

    private LocalDate deadline;

    /** 1 = mais importante. */
    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Goal() {
    }

    public Goal(Long userId, String name, BigDecimal target, LocalDate deadline, int priority) {
        this.userId = userId;
        update(name, target, deadline, priority, false);
    }

    public void update(String name, BigDecimal target, LocalDate deadline, int priority, boolean archived) {
        this.name = name;
        this.target = target;
        this.deadline = deadline;
        this.priority = priority;
        this.archived = archived;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTarget() {
        return target;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isArchived() {
        return archived;
    }
}
