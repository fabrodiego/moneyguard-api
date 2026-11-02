package com.fabrodiego.moneyguard.discord;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "discord_link_codes")
public class DiscordLinkCode {

    @Id
    private String code;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected DiscordLinkCode() {
    }

    public DiscordLinkCode(String code, Long userId, Instant expiresAt) {
        this.code = code;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getCode() {
        return code;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
