package com.fabrodiego.moneyguard.goal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fabrodiego.moneyguard.AbstractIntegrationTest;

class GoalIntegrationTest extends AbstractIntegrationTest {

    @Test
    void goalWithContributionsAndSummary() {
        String token = newUserToken();
        Long salary = createCategory(token, "Salário", "INCOME");
        Long food = createCategory(token, "Alimentação", "EXPENSE");
        createTransaction(token, salary, "5000", "2026-10-05");
        createTransaction(token, food, "1000", "2026-10-06");

        var created = post(token, "/api/goals", Map.of("name", "Entrada Carro", "target", "10000", "deadline", "2027-12-31", "priority", 1));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        Long id = ((Number) created.getBody().get("id")).longValue();
        assertThat(num(created.getBody().get("saved"))).isEqualByComparingTo("0.00");
        assertThat(created.getBody().get("monthsLeft")).isNotNull();

        Long second = ((Number) post(token, "/api/goals", Map.of("name", "Viagem", "target", "3000", "priority", 2))
            .getBody().get("id")).longValue();

        assertThat(post(token, "/api/goals/" + id + "/contributions", Map.of("amount", "500", "note", "outubro")).getStatusCode().value()).isEqualTo(201);
        var withdrawal = post(token, "/api/goals/" + id + "/contributions", Map.of("amount", "-100"));
        assertThat(withdrawal.getStatusCode().value()).isEqualTo(201);
        assertThat(post(token, "/api/goals/" + id + "/contributions", Map.of("amount", "0")).getStatusCode().value()).isEqualTo(400);
        post(token, "/api/goals/" + second + "/contributions", Map.of("amount", "300"));

        var goal = get(token, "/api/goals/" + id).getBody();
        assertThat(num(goal.get("saved"))).isEqualByComparingTo("400.00");
        assertThat(num(goal.get("remaining"))).isEqualByComparingTo("9600.00");
        assertThat(num(goal.get("percent"))).isEqualByComparingTo("4.00");
        assertThat(getList(token, "/api/goals/" + id + "/contributions")).hasSize(2);

        var summary = get(token, "/api/goals/summary").getBody();
        assertThat(num(summary.get("balance"))).isEqualByComparingTo("4000.00");
        assertThat(num(summary.get("allocated"))).isEqualByComparingTo("700.00");
        assertThat(num(summary.get("available"))).isEqualByComparingTo("3300.00");
        List<?> goals = (List<?>) summary.get("goals");
        assertThat(((Map<?, ?>) goals.get(0)).get("name")).isEqualTo("Entrada Carro");

        // reordenar prioridade e arquivar
        put(token, "/api/goals/" + id, Map.of("name", "Entrada Carro", "target", "10000", "priority", 3));
        assertThat(getList(token, "/api/goals").get(0).get("name")).isEqualTo("Viagem");
        put(token, "/api/goals/" + second, Map.of("name", "Viagem", "target", "3000", "archived", true));
        assertThat(getList(token, "/api/goals")).hasSize(1);
        assertThat(getList(token, "/api/goals?includeArchived=true")).hasSize(2);

        Long contributionId = ((Number) withdrawal.getBody().get("id")).longValue();
        assertThat(delete(token, "/api/goals/" + id + "/contributions/" + contributionId)).isEqualTo(204);
        assertThat(num(get(token, "/api/goals/" + id).getBody().get("saved"))).isEqualByComparingTo("500.00");

        String other = newUserToken();
        assertThat(get(other, "/api/goals/" + id).getStatusCode().value()).isEqualTo(404);
        assertThat(post(other, "/api/goals/" + id + "/contributions", Map.of("amount", "1")).getStatusCode().value()).isEqualTo(404);
    }
}
