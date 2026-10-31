package com.fabrodiego.moneyguard.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Uso: {@code @Convert(converter = EncryptedStringConverter.class)} em colunas bytea. */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, byte[]> {

    private final CryptoService crypto;

    public EncryptedStringConverter(CryptoService crypto) {
        this.crypto = crypto;
    }

    @Override
    public byte[] convertToDatabaseColumn(String value) {
        return crypto.encrypt(value);
    }

    @Override
    public String convertToEntityAttribute(byte[] data) {
        return crypto.decrypt(data);
    }
}
