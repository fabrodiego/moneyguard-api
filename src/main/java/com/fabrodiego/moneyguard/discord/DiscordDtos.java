package com.fabrodiego.moneyguard.discord;

import java.time.Instant;

public final class DiscordDtos {

    private DiscordDtos() {
    }

    public record LinkCodeResponse(String code, Instant expiresAt) {
    }

    public record LinkStatus(boolean linked, String discordUserId, Instant linkedAt) {
    }
}
