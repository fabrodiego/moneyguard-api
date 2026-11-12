-- Importacao de extrato e fatura (Banco Inter).

-- Apelidos: o texto que o banco escreve ("IFD*RESTAURANTE X") ligado a uma empresa cadastrada.
-- Aprendido quando o usuario aprova um lancamento importado com uma empresa.
CREATE TABLE merchant_aliases (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    merchant_id      BIGINT       NOT NULL REFERENCES merchants (id) ON DELETE CASCADE,
    normalized_alias VARCHAR(120) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, normalized_alias)
);

-- HMAC da linha do arquivo: impede importar a mesma linha duas vezes sem guardar o conteudo em claro.
ALTER TABLE transactions ADD COLUMN import_key VARCHAR(64);
CREATE UNIQUE INDEX uq_transactions_user_import_key ON transactions (user_id, import_key) WHERE import_key IS NOT NULL;

ALTER TABLE transactions DROP CONSTRAINT transactions_source_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_source_check
    CHECK (source IN ('WEB', 'DISCORD', 'RECURRENCE', 'IMPORT'));
