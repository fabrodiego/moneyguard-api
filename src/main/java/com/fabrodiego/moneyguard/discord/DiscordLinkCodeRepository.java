package com.fabrodiego.moneyguard.discord;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DiscordLinkCodeRepository extends JpaRepository<DiscordLinkCode, String> {

    @Modifying
    @Query("delete from DiscordLinkCode c where c.userId = :userId or c.expiresAt < :now")
    void deleteByUserIdOrExpired(Long userId, Instant now);
}
