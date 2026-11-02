package com.fabrodiego.moneyguard.recurrence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.fabrodiego.moneyguard.AbstractIntegrationTest;

@Import(RecurrenceIntegrationTest.ClockTestConfig.class)
class RecurrenceIntegrationTest extends AbstractIntegrationTest {

    static final ZoneId BRT = ZoneId.of("America/Sao_Paulo");

    /** Relogio que o teste avanca a mao. */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-02T13:00:00Z");

        @Override
        public ZoneId getZone() {
            return BRT;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void setDate(LocalDate date) {
            now = date.atTime(10, 0).atZone(BRT).toInstant();
        }
    }

    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    MutableClock clock;

    @Autowired
    RecurrenceService service;

    @BeforeEach
    void resetClock() {
        clock.setDate(LocalDate.of(2026, 11, 2));
    }

    Map<String, Object> body(String name, Long categoryId, String amount, String frequency, int day, Integer month) {
        Map<String, Object> m = new HashMap<>(Map.of("name", name, "categoryId", categoryId, "amount", amount,
            "frequency", frequency, "dayOfMonth", day));
        if (month != null) {
            m.put("monthOfYear", month);
        }
        return m;
    }

    List<Map<String, Object>> recurrenceTransactions(String token, String from, String to) {
        return getList(token, "/api/transactions?from=" + from + "&to=" + to).stream()
            .filter(t -> "RECURRENCE".equals(t.get("source"))).toList();
    }

    @Test
    void lancaNaDataRecuperaDiasPerdidosESemDuplicar() {
        String token = newUserToken();
        Long fun = createCategory(token, "Lazer", "EXPENSE");
        Long salary = createCategory(token, "Salário", "INCOME");

        // cobranca hoje: lanca na hora
        var streaming = post(token, "/api/recurrences", body("Streaming", fun, "55.90", "MONTHLY", 2, null));
        assertThat(streaming.getStatusCode().value()).isEqualTo(201);
        assertThat(streaming.getBody().get("nextDueOn")).isEqualTo("2026-12-02");
        var today = recurrenceTransactions(token, "2026-11-01", "2026-11-30");
        assertThat(today).hasSize(1);
        assertThat(today.get(0).get("description")).isEqualTo("Streaming");
        assertThat(today.get(0).get("occurredOn")).isEqualTo("2026-11-02");

        var gym = post(token, "/api/recurrences", body("Academia", fun, "99.90", "MONTHLY", 10, null)).getBody();
        assertThat(gym.get("nextDueOn")).isEqualTo("2026-11-10");
        post(token, "/api/recurrences", body("Salário fixo", salary, "6500", "MONTHLY", 5, null));

        var domain = post(token, "/api/recurrences", body("Domínio", fun, "120", "YEARLY", 15, 3)).getBody();
        assertThat(domain.get("nextDueOn")).isEqualTo("2027-03-15");
        assertThat(num(domain.get("monthlyAmount"))).isEqualByComparingTo("10.00");
        assertThat(recurrenceTransactions(token, "2026-11-01", "2026-11-30")).hasSize(1);

        var summary = get(token, "/api/recurrences/summary").getBody();
        assertThat(num(summary.get("monthlyExpenses"))).isEqualByComparingTo("165.80");
        assertThat(num(summary.get("monthlyIncomes"))).isEqualByComparingTo("6500");
        assertThat((List<?>) summary.get("next30Days")).hasSize(3); // academia 10/11, salario 05/11, streaming 02/12

        // API "fora do ar" ate 15/12: recupera tudo que venceu
        clock.setDate(LocalDate.of(2026, 12, 15));
        service.launchAllDue();
        assertThat(recurrenceTransactions(token, "2026-11-01", "2026-12-31")).hasSize(6);
        // rodar de novo nao duplica
        assertThat(service.launchAllDue()).isZero();
        assertThat(recurrenceTransactions(token, "2026-11-01", "2026-12-31")).hasSize(6);

        var insight = get(token, "/api/insights?period=MONTH&year=2026&number=12").getBody();
        assertThat(num(((Map<?, ?>) insight.get("totals")).get("income"))).isEqualByComparingTo("6500");
    }

    @Test
    void pausarEncerrarEExcluir() {
        String token = newUserToken();
        Long fun = createCategory(token, "Lazer", "EXPENSE");
        Long id = ((Number) post(token, "/api/recurrences", body("Música", fun, "21.90", "MONTHLY", 20, null))
            .getBody().get("id")).longValue();

        Map<String, Object> paused = body("Música", fun, "21.90", "MONTHLY", 20, null);
        paused.put("active", false);
        assertThat(put(token, "/api/recurrences/" + id, paused).getBody().get("active")).isEqualTo(false);
        clock.setDate(LocalDate.of(2026, 11, 25));
        service.launchAllDue();
        assertThat(recurrenceTransactions(token, "2026-11-01", "2026-11-30")).isEmpty();

        // reativar nao lanca retroativo (20/11 ja passou): proxima e 20/12
        var resumed = put(token, "/api/recurrences/" + id, body("Música", fun, "21.90", "MONTHLY", 20, null)).getBody();
        assertThat(resumed.get("nextDueOn")).isEqualTo("2026-12-20");

        // data final antes da proxima cobranca: encerra
        Map<String, Object> ended = body("Música", fun, "21.90", "MONTHLY", 20, null);
        ended.put("startDate", "2026-11-01");
        ended.put("endDate", "2026-12-01");
        assertThat(put(token, "/api/recurrences/" + id, ended).getBody().get("nextDueOn")).isNull();

        // categoria em uso por assinatura nao pode ser excluida
        assertThat(delete(token, "/api/categories/" + fun)).isEqualTo(409);

        assertThat(delete(token, "/api/recurrences/" + id)).isEqualTo(204);
        assertThat(getList(token, "/api/recurrences")).isEmpty();
    }

    @Test
    void validacaoEIsolamento() {
        String token = newUserToken();
        Long fun = createCategory(token, "Lazer", "EXPENSE");
        assertThat(post(token, "/api/recurrences", body("Anual", fun, "100", "YEARLY", 10, null)).getStatusCode().value()).isEqualTo(400);
        assertThat(post(token, "/api/recurrences", body("X", fun, "100", "MONTHLY", 32, null)).getStatusCode().value()).isEqualTo(400);
        Map<String, Object> badEnd = body("X", fun, "100", "MONTHLY", 10, null);
        badEnd.put("startDate", "2026-12-01");
        badEnd.put("endDate", "2026-11-01");
        assertThat(post(token, "/api/recurrences", badEnd).getStatusCode().value()).isEqualTo(400);

        Long id = ((Number) post(token, "/api/recurrences", body("Seguro", fun, "80", "MONTHLY", 28, null))
            .getBody().get("id")).longValue();
        String other = newUserToken();
        assertThat(get(other, "/api/recurrences/" + id).getStatusCode().value()).isEqualTo(404);
        assertThat(getList(other, "/api/recurrences")).isEmpty();
        assertThat(post(other, "/api/recurrences", body("Y", fun, "1", "MONTHLY", 1, null)).getStatusCode().value()).isEqualTo(404);
    }
}
