package com.fabrodiego.moneyguard.discord;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma conta do Discord por usuario, e vice-versa. */
@Entity
@Table(name = "discord_links")
public class DiscordLink {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "discord_user_id", nullable = false, unique = true)
    private String discordUserId;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected DiscordLink() {
    }

    public DiscordLink(Long userId, String discordUserId, Instant linkedAt) {
        this.userId = userId;
        this.discordUserId = discordUserId;
        this.linkedAt = linkedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDiscordUserId() {
        return discordUserId;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
