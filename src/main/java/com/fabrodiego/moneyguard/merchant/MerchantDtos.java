package com.fabrodiego.moneyguard.merchant;

import com.fabrodiego.moneyguard.category.CategoryDtos.CategoryResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class MerchantDtos {

    private MerchantDtos() {
    }

    public record MerchantRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull Long categoryId) {
    }

    public record MerchantResponse(Long id, String name, CategoryResponse category) {

        public static MerchantResponse from(Merchant m) {
            return new MerchantResponse(m.getId(), m.getName(), CategoryResponse.from(m.getCategory()));
        }
    }
}
