package com.fabrodiego.moneyguard.bot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fabrodiego.moneyguard.AbstractIntegrationTest;

class BotIntegrationTest extends AbstractIntegrationTest {

    @SuppressWarnings("unchecked")
    ResponseEntity<Map<String, Object>> bot(String method, String uri, Object body, String key) {
        var spec = http.method(org.springframework.http.HttpMethod.valueOf(method)).uri(uri);
        if (key != null) {
            spec = spec.header("X-Bot-Key", key);
        }
        if (body != null) {
            spec = spec.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) spec.retrieve().toEntity(Map.class);
    }

    ResponseEntity<Map<String, Object>> bot(String method, String uri, Object body) {
        return bot(method, uri, body, BOT_KEY);
    }

    String linkCode(String token) {
        return (String) post(token, "/api/discord/link-code", Map.of()).getBody().get("code");
    }

    @Test
    void fluxoCompleto() {
        String token = newUserToken();
        Long food = createCategory(token, "Alimentação", "EXPENSE");
        post(token, "/api/merchants", Map.of("name", "Cantina Central", "categoryId", food));
        String discord = "123456789012345678";

        // sem vinculo
        var notLinked = bot("POST", "/api/bot/parse", Map.of("discordUserId", discord, "text", "gastei 8 na cantina central"));
        assertThat(notLinked.getStatusCode().value()).isEqualTo(404);
        assertThat(notLinked.getBody().get("title")).isEqualTo("DISCORD_NOT_LINKED");

        // vincular
        String code = linkCode(token);
        assertThat(code).hasSize(6);
        var link = bot("POST", "/api/bot/link", Map.of("discordUserId", discord, "code", code.toLowerCase()));
        assertThat(link.getStatusCode().value()).isEqualTo(200);
        assertThat(link.getBody().get("userName")).isEqualTo("Teste");
        assertThat(bot("POST", "/api/bot/link", Map.of("discordUserId", discord, "code", code)).getStatusCode().value()).isEqualTo(400);
        assertThat(get(token, "/api/discord/link").getBody().get("linked")).isEqualTo(true);

        // interpretar e lancar
        var parsed = bot("POST", "/api/bot/parse", Map.of("discordUserId", discord, "text", "acabei de gastar 8 reais na cantina central")).getBody();
        assertThat(num(parsed.get("amount"))).isEqualByComparingTo("8");
        assertThat(((Map<?, ?>) parsed.get("merchant")).get("name")).isEqualTo("Cantina Central");
        assertThat(((Map<?, ?>) parsed.get("category")).get("name")).isEqualTo("Alimentação");

        var created = bot("POST", "/api/bot/transactions", Map.of(
            "discordUserId", discord, "categoryId", food, "merchantId", ((Number) ((Map<?, ?>) parsed.get("merchant")).get("id")).longValue(),
            "amount", 8, "occurredOn", parsed.get("occurredOn")));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody().get("source")).isEqualTo("DISCORD");

        List<?> cats = http.get().uri("/api/bot/categories?discordUserId=" + discord + "&type=EXPENSE")
            .header("X-Bot-Key", BOT_KEY).retrieve().body(List.class);
        assertThat(cats).hasSize(1);

        var summary = bot("GET", "/api/bot/summary?discordUserId=" + discord, null).getBody();
        assertThat(num(((Map<?, ?>) summary.get("month")).get("expense"))).isEqualByComparingTo("8");
        assertThat(summary.get("userName")).isEqualTo("Teste");

        // desfazer
        Long txId = ((Number) created.getBody().get("id")).longValue();
        assertThat(bot("DELETE", "/api/bot/transactions/" + txId + "?discordUserId=" + discord, null).getStatusCode().value()).isEqualTo(204);

        // desvincular pelo bot
        assertThat(bot("POST", "/api/bot/unlink", Map.of("discordUserId", discord)).getStatusCode().value()).isEqualTo(204);
        assertThat(get(token, "/api/discord/link").getBody().get("linked")).isEqualTo(false);
    }

    @Test
    void discordDeOutraContaNaoVincula() {
        String discord = "987654321098765432";
        String first = newUserToken();
        bot("POST", "/api/bot/link", Map.of("discordUserId", discord, "code", linkCode(first)));

        String second = newUserToken();
        var conflict = bot("POST", "/api/bot/link", Map.of("discordUserId", discord, "code", linkCode(second)));
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void seguranca() {
        String token = newUserToken();
        String discord = "111111111111111111";
        bot("POST", "/api/bot/link", Map.of("discordUserId", discord, "code", linkCode(token)));

        // sem chave ou chave errada
        assertThat(bot("GET", "/api/bot/summary?discordUserId=" + discord, null, null).getStatusCode().value()).isEqualTo(401);
        assertThat(bot("GET", "/api/bot/summary?discordUserId=" + discord, null, "errada").getStatusCode().value()).isEqualTo(401);
        // token de usuario nao abre rota do bot
        assertThat(get(token, "/api/bot/summary?discordUserId=" + discord).getStatusCode().value()).isEqualTo(403);
        // chave do bot nao abre rota de usuario
        assertThat(bot("GET", "/api/transactions", null).getStatusCode().value()).isEqualTo(401);
        // id do discord invalido
        assertThat(bot("GET", "/api/bot/summary?discordUserId=abc", null).getStatusCode().value()).isEqualTo(400);
        // codigo invalido
        assertThat(bot("POST", "/api/bot/link", Map.of("discordUserId", discord, "code", "XXXXXX")).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void assinaturaPeloBot() {
        String token = newUserToken();
        Long fun = createCategory(token, "Lazer", "EXPENSE");
        String discord = "222222222222222222";
        bot("POST", "/api/bot/link", Map.of("discordUserId", discord, "code", linkCode(token)));
        int today = java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).getDayOfMonth();

        var parsed = bot("POST", "/api/bot/parse", Map.of("discordUserId", discord,
            "text", "assinei streaming 55,90 todo dia " + today)).getBody();
        Map<?, ?> rec = (Map<?, ?>) parsed.get("recurrence");
        assertThat(rec.get("frequency")).isEqualTo("MONTHLY");
        assertThat(rec.get("dayOfMonth")).isEqualTo(today);
        assertThat(parsed.get("description")).isEqualTo("Streaming");

        var created = bot("POST", "/api/bot/recurrences", Map.of("discordUserId", discord, "name", "Streaming",
            "categoryId", fun, "amount", 55.9, "frequency", "MONTHLY", "dayOfMonth", today));
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        assertThat(created.getBody().get("launchedToday")).isEqualTo(true);

        var list = bot("GET", "/api/bot/recurrences?discordUserId=" + discord, null).getBody();
        assertThat((List<?>) list.get("active")).hasSize(1);
        assertThat(num(((Map<?, ?>) list.get("summary")).get("monthlyExpenses"))).isEqualByComparingTo("55.90");

        // anual sem mes e invalido
        assertThat(bot("POST", "/api/bot/recurrences", Map.of("discordUserId", discord, "name", "X",
            "categoryId", fun, "amount", 10, "frequency", "YEARLY", "dayOfMonth", 1)).getStatusCode().value()).isEqualTo(400);
    }
}
