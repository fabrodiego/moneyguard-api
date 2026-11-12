package com.fabrodiego.moneyguard.merchant;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Texto que o banco usa para uma empresa ("ifd*restaurante x"), aprendido na importacao. */
@Entity
@Table(name = "merchant_aliases")
public class MerchantAlias {

    public static final int MAX_LENGTH = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "normalized_alias", nullable = false, updatable = false)
    private String normalizedAlias;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MerchantAlias() {
    }

    public MerchantAlias(Long userId, Merchant merchant, String normalizedAlias) {
        this.userId = userId;
        this.merchant = merchant;
        this.normalizedAlias = normalizedAlias;
    }

    public void pointTo(Merchant merchant) {
        this.merchant = merchant;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public String getNormalizedAlias() {
        return normalizedAlias;
    }
}
