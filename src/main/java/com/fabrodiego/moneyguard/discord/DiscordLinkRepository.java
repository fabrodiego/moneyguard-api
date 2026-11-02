package com.fabrodiego.moneyguard.discord;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscordLinkRepository extends JpaRepository<DiscordLink, Long> {

    Optional<DiscordLink> findByDiscordUserId(String discordUserId);
}
