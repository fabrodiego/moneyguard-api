package com.fabrodiego.moneyguard.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.fabrodiego.moneyguard.config.AppProperties;

/**
 * Cifra valores sensiveis (AES-256-GCM) antes de irem para o banco.
 * Formato gravado: [versao 1 byte][IV 12 bytes][ciphertext + tag 16 bytes].
 */
@Service
public class CryptoService {

    private static final byte VERSION = 1;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(AppProperties props) {
        byte[] bytes = Base64.getDecoder().decode(props.crypto().key());
        if (bytes.length != 32) {
            throw new IllegalStateException("CRYPTO_KEY precisa ter exatamente 32 bytes (base64)");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    public byte[] encrypt(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(1 + IV_LENGTH + encrypted.length)
                .put(VERSION).put(iv).put(encrypted).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar valor", e);
        }
    }

    public String decrypt(byte[] data) {
        if (data == null) {
            return null;
        }
        if (data.length < 1 + IV_LENGTH + TAG_BITS / 8 || data[0] != VERSION) {
            throw new IllegalStateException("Valor cifrado em formato invalido");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 1, IV_LENGTH));
            byte[] plain = cipher.doFinal(data, 1 + IV_LENGTH, data.length - 1 - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao decifrar valor", e);
        }
    }
}
