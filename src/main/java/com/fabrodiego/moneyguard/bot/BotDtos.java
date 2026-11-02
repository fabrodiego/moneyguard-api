package com.fabrodiego.moneyguard.bot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fabrodiego.moneyguard.goal.GoalDtos.GoalSummary;
import com.fabrodiego.moneyguard.insight.InsightDtos.CategoryTotal;
import com.fabrodiego.moneyguard.insight.InsightDtos.Totals;
import com.fabrodiego.moneyguard.transaction.TransactionType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class BotDtos {

    /** Snowflake do Discord: so digitos. */
    static final String DISCORD_ID = "\\d{5,32}";

    private BotDtos() {
    }

    public record LinkRequest(@NotBlank @Pattern(regexp = DISCORD_ID) String discordUserId,
                              @NotBlank @Size(max = 12) String code) {
    }

    public record DiscordUserRequest(@NotBlank @Pattern(regexp = DISCORD_ID) String discordUserId) {
    }

    public record LinkResponse(String userName) {
    }

    public record ParseRequest(@NotBlank @Pattern(regexp = DISCORD_ID) String discordUserId,
                               @NotBlank @Size(max = 500) String text) {
    }

    public record Ref(Long id, String name) {
    }

    /** amount null = a frase nao tinha valor. category null = o bot pede para escolher. */
    public record ParseResponse(BigDecimal amount, TransactionType type, LocalDate occurredOn, String description,
                                Ref category, Ref merchant) {
    }

    public record BotTransactionRequest(
        @NotBlank @Pattern(regexp = DISCORD_ID) String discordUserId,
        @NotNull Long categoryId,
        Long merchantId,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description,
        LocalDate occurredOn) {
    }

    public record Summary(String userName, String monthLabel, Totals month, List<CategoryTotal> topExpenses,
                          GoalSummary goals) {
    }
}
