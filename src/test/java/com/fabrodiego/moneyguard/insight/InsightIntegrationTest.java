package com.fabrodiego.moneyguard.insight;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fabrodiego.moneyguard.AbstractIntegrationTest;

class InsightIntegrationTest extends AbstractIntegrationTest {

    @Test
    void periodsAndCategoryBreakdown() {
        String token = newUserToken();
        Long salary = createCategory(token, "Salário", "INCOME");
        Long food = createCategory(token, "Alimentação", "EXPENSE");
        Long fun = createCategory(token, "Lazer", "EXPENSE");

        createTransaction(token, salary, "5000", "2026-10-05");
        createTransaction(token, food, "300", "2026-10-10");
        createTransaction(token, food, "100", "2026-11-02");
        createTransaction(token, fun, "100", "2026-10-20");
        createTransaction(token, salary, "4000", "2026-09-05");
        createTransaction(token, food, "1000", "2026-09-10");

        var month = get(token, "/api/insights?period=MONTH&year=2026&number=10").getBody();
        Map<?, ?> totals = (Map<?, ?>) month.get("totals");
        assertThat(num(totals.get("income"))).isEqualByComparingTo("5000.00");
        assertThat(num(totals.get("expense"))).isEqualByComparingTo("400.00");
        assertThat(num(totals.get("balance"))).isEqualByComparingTo("4600.00");
        assertThat(num(((Map<?, ?>) month.get("previous")).get("balance"))).isEqualByComparingTo("3000.00");
        assertThat((List<?>) month.get("series")).hasSize(31);

        List<?> expenses = (List<?>) month.get("expensesByCategory");
        Map<?, ?> top = (Map<?, ?>) expenses.get(0);
        assertThat(top.get("name")).isEqualTo("Alimentação");
        assertThat(num(top.get("percent"))).isEqualByComparingTo("75.00");

        var quarter = get(token, "/api/insights?period=QUARTER&year=2026&number=4").getBody();
        assertThat(quarter.get("from")).isEqualTo("2026-10-01");
        assertThat(quarter.get("to")).isEqualTo("2026-12-31");
        assertThat(num(((Map<?, ?>) quarter.get("totals")).get("expense"))).isEqualByComparingTo("500.00");
        assertThat(num(((Map<?, ?>) quarter.get("previous")).get("income"))).isEqualByComparingTo("4000.00");
        assertThat((List<?>) quarter.get("series")).hasSize(3);

        var semester = get(token, "/api/insights?period=SEMESTER&year=2026&number=2").getBody();
        assertThat(num(((Map<?, ?>) semester.get("totals")).get("income"))).isEqualByComparingTo("9000.00");
        assertThat((List<?>) semester.get("series")).hasSize(6);

        var year = get(token, "/api/insights?period=YEAR&year=2026").getBody();
        assertThat(num(((Map<?, ?>) year.get("totals")).get("balance"))).isEqualByComparingTo("7500.00");
        assertThat((List<?>) year.get("series")).hasSize(12);
    }

    @Test
    void invalidNumberAndPeriod() {
        String token = newUserToken();
        assertThat(get(token, "/api/insights?period=QUARTER&year=2026&number=5").getStatusCode().value()).isEqualTo(400);
        assertThat(get(token, "/api/insights?period=WEEK").getStatusCode().value()).isEqualTo(400);
        assertThat(get(token, "/api/insights").getStatusCode().value()).isEqualTo(200);
    }
}
