package com.fabrodiego.moneyguard.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fabrodiego.moneyguard.AbstractIntegrationTest;

class TransactionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void crudFilterAndTypeFromCategory() {
        String token = newUserToken();
        Long food = createCategory(token, "Alimentação", "EXPENSE");
        Long salary = createCategory(token, "Salário", "INCOME");
        Long merchant = ((Number) post(token, "/api/merchants", Map.of("name", "Sodexo", "categoryId", food))
            .getBody().get("id")).longValue();

        var created = post(token, "/api/transactions", Map.of(
            "categoryId", food, "merchantId", merchant, "amount", "8", "description", "Almoço", "occurredOn", "2026-10-10"));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody().get("type")).isEqualTo("EXPENSE");
        assertThat(num(created.getBody().get("amount"))).isEqualByComparingTo("8.00");
        assertThat(created.getBody().get("source")).isEqualTo("WEB");
        assertThat(((Map<?, ?>) created.getBody().get("merchant")).get("name")).isEqualTo("Sodexo");
        Long id = ((Number) created.getBody().get("id")).longValue();

        createTransaction(token, salary, "5000", "2026-10-05");
        createTransaction(token, food, "30", "2026-09-20");

        assertThat(getList(token, "/api/transactions?from=2026-10-01&to=2026-10-31")).hasSize(2);
        assertThat(getList(token, "/api/transactions?from=2026-10-01&to=2026-10-31&type=INCOME")).hasSize(1);
        assertThat(getList(token, "/api/transactions?from=2026-09-01&to=2026-10-31&categoryId=" + food)).hasSize(2);
        assertThat(get(token, "/api/transactions?from=2026-10-31&to=2026-10-01").getStatusCode().value()).isEqualTo(400);

        // trocar para categoria de renda muda o tipo
        var updated = put(token, "/api/transactions/" + id, Map.of("categoryId", salary, "amount", "9.5", "occurredOn", "2026-10-10"));
        assertThat(updated.getBody().get("type")).isEqualTo("INCOME");
        assertThat(num(updated.getBody().get("amount"))).isEqualByComparingTo("9.50");
        assertThat(updated.getBody().get("merchant")).isNull();

        assertThat(delete(token, "/api/transactions/" + id)).isEqualTo(204);
        assertThat(get(token, "/api/transactions/" + id).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void validation() {
        String token = newUserToken();
        Long food = createCategory(token, "Alimentação", "EXPENSE");
        assertThat(post(token, "/api/transactions", Map.of("categoryId", food, "amount", "-1")).getStatusCode().value()).isEqualTo(400);
        assertThat(post(token, "/api/transactions", Map.of("categoryId", food, "amount", "1.234")).getStatusCode().value()).isEqualTo(400);
        assertThat(post(token, "/api/transactions", Map.of("amount", "1")).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void amountAndDescriptionAreEncryptedInDatabase() {
        String token = newUserToken();
        Long food = createCategory(token, "Alimentação", "EXPENSE");
        var created = post(token, "/api/transactions", Map.of(
            "categoryId", food, "amount", "1234.56", "description", "segredo-teste", "occurredOn", "2026-10-10"));
        Long id = ((Number) created.getBody().get("id")).longValue();

        Map<String, Object> row = jdbc.queryForMap("select amount_enc, description_enc from moneyguard.transactions where id = ?", id);
        String amountRaw = new String((byte[]) row.get("amount_enc"), StandardCharsets.ISO_8859_1);
        String descRaw = new String((byte[]) row.get("description_enc"), StandardCharsets.ISO_8859_1);
        assertThat(amountRaw).doesNotContain("1234");
        assertThat(descRaw).doesNotContain("segredo");
        assertThat(((byte[]) row.get("amount_enc"))[0]).isEqualTo((byte) 1);
    }
}
