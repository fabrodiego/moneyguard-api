-- Gastos e rendas recorrentes (assinaturas, contas fixas, salario).
-- Nome e valor cifrados pela aplicacao; datas em claro para o lancador diario.

CREATE TABLE recurrences (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id    BIGINT      NOT NULL REFERENCES categories (id),
    merchant_id    BIGINT      REFERENCES merchants (id) ON DELETE SET NULL,
    type           VARCHAR(10) NOT NULL CHECK (type IN ('EXPENSE', 'INCOME')),
    name_enc       BYTEA       NOT NULL,
    amount_enc     BYTEA       NOT NULL,
    frequency      VARCHAR(10) NOT NULL CHECK (frequency IN ('MONTHLY', 'YEARLY')),
    day_of_month   INT         NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    month_of_year  INT         CHECK (month_of_year BETWEEN 1 AND 12),
    start_date     DATE        NOT NULL,
    end_date       DATE,
    next_due_on    DATE,
    active         BOOLEAN     NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (frequency = 'MONTHLY' OR month_of_year IS NOT NULL)
);
CREATE INDEX ix_recurrences_due ON recurrences (next_due_on) WHERE active;
CREATE INDEX ix_recurrences_user ON recurrences (user_id);

ALTER TABLE transactions ADD COLUMN recurrence_id BIGINT REFERENCES recurrences (id) ON DELETE SET NULL;
ALTER TABLE transactions DROP CONSTRAINT transactions_source_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_source_check CHECK (source IN ('WEB', 'DISCORD', 'RECURRENCE'));
-- Garante no banco que a mesma assinatura nunca lanca duas vezes no mesmo dia.
CREATE UNIQUE INDEX uq_transactions_recurrence_date ON transactions (recurrence_id, occurred_on) WHERE recurrence_id IS NOT NULL;
