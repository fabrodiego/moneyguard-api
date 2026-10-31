package com.fabrodiego.moneyguard.category;

import com.fabrodiego.moneyguard.transaction.TransactionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CreateCategoryRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull TransactionType type,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "cor no formato #RRGGBB") String color) {
    }

    /** O tipo nao muda: lancamentos ja gravados dependem dele. */
    public record UpdateCategoryRequest(
        @NotBlank @Size(max = 60) String name,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "cor no formato #RRGGBB") String color) {
    }

    public record CategoryResponse(Long id, String name, TransactionType type, String color) {

        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getType(), c.getColor());
        }
    }
}
