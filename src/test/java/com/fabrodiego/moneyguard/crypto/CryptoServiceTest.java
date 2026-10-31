package com.fabrodiego.moneyguard.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.fabrodiego.moneyguard.config.AppProperties;

class CryptoServiceTest {

    private final CryptoService crypto = new CryptoService(
        new AppProperties(null, new AppProperties.Crypto(Base64.getEncoder().encodeToString(new byte[32])), null));

    @Test
    void roundTrip() {
        byte[] enc = crypto.encrypt("1234.56");
        assertThat(crypto.decrypt(enc)).isEqualTo("1234.56");
    }

    @Test
    void sameValueGivesDifferentCiphertext() {
        assertThat(crypto.encrypt("10.00")).isNotEqualTo(crypto.encrypt("10.00"));
    }

    @Test
    void tamperedDataIsRejected() {
        byte[] enc = crypto.encrypt("10.00");
        enc[enc.length - 1] ^= 1;
        assertThatThrownBy(() -> crypto.decrypt(enc)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWrongKeySize() {
        assertThatThrownBy(() -> new CryptoService(
            new AppProperties(null, new AppProperties.Crypto(Base64.getEncoder().encodeToString(new byte[16])), null)))
            .isInstanceOf(IllegalStateException.class);
    }
}
