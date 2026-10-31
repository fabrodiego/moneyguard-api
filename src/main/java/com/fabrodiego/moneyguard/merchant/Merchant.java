package com.fabrodiego.moneyguard.merchant;

import java.time.Instant;

import com.fabrodiego.moneyguard.category.Category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Empresa conhecida: liga um nome ("sodexo") a uma categoria, para o bot classificar sozinho. */
@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Merchant() {
    }

    public Merchant(Long userId, String name, String normalizedName, Category category) {
        this.userId = userId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.category = category;
    }

    public void update(String name, String normalizedName, Category category) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.category = category;
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

    public String getNormalizedName() {
        return normalizedName;
    }

    public Category getCategory() {
        return category;
    }
}
