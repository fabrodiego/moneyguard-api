-- Valores financeiros e textos livres sao gravados cifrados (colunas *_enc, AES-256-GCM pela aplicacao).
-- Datas, tipos e chaves ficam em claro para permitir filtros por periodo e por categoria.

CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(60)  NOT NULL,
    type       VARCHAR(10)  NOT NULL CHECK (type IN ('EXPENSE', 'INCOME')),
    color      VARCHAR(7),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_categories_user_name_type ON categories (user_id, lower(name), type);

-- Empresas conhecidas: o bot usa o nome normalizado para achar a categoria.
CREATE TABLE merchants (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(80)  NOT NULL,
    normalized_name VARCHAR(80)  NOT NULL,
    category_id     BIGINT       NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, normalized_name)
);

CREATE TABLE transactions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id     BIGINT      NOT NULL REFERENCES categories (id),
    merchant_id     BIGINT      REFERENCES merchants (id) ON DELETE SET NULL,
    type            VARCHAR(10) NOT NULL CHECK (type IN ('EXPENSE', 'INCOME')),
    amount_enc      BYTEA       NOT NULL,
    description_enc BYTEA,
    occurred_on     DATE        NOT NULL,
    source          VARCHAR(10) NOT NULL DEFAULT 'WEB' CHECK (source IN ('WEB', 'DISCORD')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_transactions_user_date ON transactions (user_id, occurred_on);

CREATE TABLE goals (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(80) NOT NULL,
    target_enc BYTEA       NOT NULL,
    deadline   DATE,
    priority   INT         NOT NULL DEFAULT 1,
    archived   BOOLEAN     NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE goal_contributions (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    goal_id        BIGINT      NOT NULL REFERENCES goals (id) ON DELETE CASCADE,
    amount_enc     BYTEA       NOT NULL,
    note_enc       BYTEA,
    contributed_on DATE        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_goal_contributions_goal ON goal_contributions (goal_id);

-- Vinculo da conta do Discord (bot D7) com o usuario.
CREATE TABLE discord_links (
    user_id         BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    discord_user_id VARCHAR(32) NOT NULL UNIQUE,
    linked_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE discord_link_codes (
    code       VARCHAR(12) PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL
);
