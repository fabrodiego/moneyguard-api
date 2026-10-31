package com.fabrodiego.moneyguard.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fabrodiego.moneyguard.category.CategoryDtos.CategoryResponse;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    /** occurredOn vazio = hoje. O tipo (gasto/renda) vem da categoria. */
    public record TransactionRequest(
        @NotNull Long categoryId,
        Long merchantId,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description,
        LocalDate occurredOn) {
    }

    public record MerchantRef(Long id, String name) {
    }

    public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate occurredOn,
        TransactionSource source,
        CategoryResponse category,
        MerchantRef merchant) {

        public static TransactionResponse from(Transaction t) {
            MerchantRef merchant = t.getMerchant() == null ? null
                : new MerchantRef(t.getMerchant().getId(), t.getMerchant().getName());
            return new TransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getDescription(),
                t.getOccurredOn(), t.getSource(), CategoryResponse.from(t.getCategory()), merchant);
        }
    }
}
