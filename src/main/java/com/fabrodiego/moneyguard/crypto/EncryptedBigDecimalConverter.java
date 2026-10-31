package com.fabrodiego.moneyguard.crypto;

import java.math.BigDecimal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Uso: {@code @Convert(converter = EncryptedBigDecimalConverter.class)} em colunas bytea. */
@Converter
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, byte[]> {

    private final CryptoService crypto;

    public EncryptedBigDecimalConverter(CryptoService crypto) {
        this.crypto = crypto;
    }

    @Override
    public byte[] convertToDatabaseColumn(BigDecimal value) {
        return value == null ? null : crypto.encrypt(value.toPlainString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(byte[] data) {
        return data == null ? null : new BigDecimal(crypto.decrypt(data));
    }
}
