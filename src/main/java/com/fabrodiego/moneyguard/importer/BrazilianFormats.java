package com.fabrodiego.moneyguard.importer;

import java.math.BigDecimal;

import com.fabrodiego.moneyguard.common.Money;

final class BrazilianFormats {

    private BrazilianFormats() {
    }

    /** "-1.580,92" -> -1580.92 */
    static BigDecimal money(String text) {
        String clean = text.replace("R$", "").replace(" ", "").replace(".", "").replace(',', '.');
        try {
            return Money.of(new BigDecimal(clean));
        } catch (NumberFormatException e) {
            throw new ImportFormatException("Valor invalido no arquivo: " + text.trim());
        }
    }

    /** Junta espacos repetidos ("Pista 4      Sao Jose") e tira as pontas. */
    static String collapse(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }
}
