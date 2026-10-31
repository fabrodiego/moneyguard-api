package com.fabrodiego.moneyguard.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fabrodiego.moneyguard.AbstractIntegrationTest;

class CategoryMerchantIntegrationTest extends AbstractIntegrationTest {

    @Test
    void categoryCrudAndRules() {
        String token = newUserToken();

        var created = post(token, "/api/categories", Map.of("name", "Alimentação", "type", "EXPENSE", "color", "#06B6D4"));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        Long id = ((Number) created.getBody().get("id")).longValue();

        // mesmo nome com outro tipo e permitido; mesmo nome e tipo nao
        assertThat(post(token, "/api/categories", Map.of("name", "alimentação", "type", "EXPENSE")).getStatusCode().value()).isEqualTo(409);
        assertThat(post(token, "/api/categories", Map.of("name", "Alimentação", "type", "INCOME")).getStatusCode().value()).isEqualTo(201);
        assertThat(post(token, "/api/categories", Map.of("name", "X", "type", "EXPENSE", "color", "azul")).getStatusCode().value()).isEqualTo(400);

        var updated = put(token, "/api/categories/" + id, Map.of("name", "Comida", "color", "#000000"));
        assertThat(updated.getBody().get("name")).isEqualTo("Comida");
        assertThat(updated.getBody().get("type")).isEqualTo("EXPENSE");

        assertThat(getList(token, "/api/categories?type=EXPENSE")).hasSize(1);
        assertThat(getList(token, "/api/categories")).hasSize(2);

        // categoria com lancamento nao pode ser excluida
        createTransaction(token, id, "10.00", "2026-10-01");
        assertThat(delete(token, "/api/categories/" + id)).isEqualTo(409);
    }

    @Test
    void usersAreIsolated() {
        String owner = newUserToken();
        String other = newUserToken();
        Long id = createCategory(owner, "Lazer", "EXPENSE");

        assertThat(get(other, "/api/categories/" + id).getStatusCode().value()).isEqualTo(404);
        assertThat(getList(other, "/api/categories")).isEmpty();
        assertThat(delete(other, "/api/categories/" + id)).isEqualTo(404);
        assertThat(post(other, "/api/transactions", Map.of("categoryId", id, "amount", "5")).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void merchantCrudWithNormalizedNames() {
        String token = newUserToken();
        Long food = createCategory(token, "Alimentação", "EXPENSE");
        Long transport = createCategory(token, "Transporte", "EXPENSE");

        var created = post(token, "/api/merchants", Map.of("name", "Sodexo", "categoryId", food));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        Long id = ((Number) created.getBody().get("id")).longValue();

        assertThat(post(token, "/api/merchants", Map.of("name", "  SODÉXO ", "categoryId", food)).getStatusCode().value()).isEqualTo(409);

        var updated = put(token, "/api/merchants/" + id, Map.of("name", "Sodexo", "categoryId", transport));
        assertThat(((Map<?, ?>) updated.getBody().get("category")).get("name")).isEqualTo("Transporte");

        assertThat(getList(token, "/api/merchants")).hasSize(1);
        assertThat(delete(token, "/api/merchants/" + id)).isEqualTo(204);
        assertThat(getList(token, "/api/merchants")).isEmpty();
    }
}
