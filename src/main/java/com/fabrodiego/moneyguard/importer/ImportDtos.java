package com.fabrodiego.moneyguard.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fabrodiego.moneyguard.merchant.MerchantAlias;
import com.fabrodiego.moneyguard.merchant.MerchantDtos.MerchantResponse;
import com.fabrodiego.moneyguard.transaction.TransactionType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class ImportDtos {

    private ImportDtos() {
    }

    /**
     * Uma linha para o usuario revisar. Nada foi gravado ainda.
     * description = texto do banco (vira apelido da empresa); suggestedDescription ja inclui a parcela.
     * ownTransfer = Pix para o proprio nome (sugestao: pular). alreadyImported = ja aprovado antes.
     */
    public record PreviewItem(
        String importKey,
        LocalDate occurredOn,
        String description,
        String suggestedDescription,
        BigDecimal amount,
        TransactionType type,
        String history,
        String installment,
        boolean ownTransfer,
        boolean alreadyImported,
        MerchantResponse suggestion) {
    }

    /** warning != null quando a soma lida nao bate com o total impresso na fatura. */
    public record PreviewResponse(
        ImportKind kind,
        List<PreviewItem> items,
        List<ParsedFile.Ignored> ignored,
        BigDecimal declaredTotal,
        BigDecimal parsedTotal,
        String warning) {
    }

    /**
     * Aprova uma linha: vira lancamento (origem IMPORT). type precisa bater com a categoria.
     * alias = texto original do banco; com merchantId, a empresa passa a ser sugerida para ele.
     */
    public record ApproveRequest(
        @NotNull @Pattern(regexp = "[0-9a-f]{64}") String importKey,
        @NotNull TransactionType type,
        @NotNull Long categoryId,
        Long merchantId,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description,
        @NotNull LocalDate occurredOn,
        @Size(max = MerchantAlias.MAX_LENGTH) String alias) {
    }
}
