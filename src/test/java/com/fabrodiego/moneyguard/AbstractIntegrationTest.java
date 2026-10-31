package com.fabrodiego.moneyguard;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/** Sobe um Postgres embarcado (sem Docker) compartilhado por todos os testes de integracao. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres") + "&currentSchema=moneyguard");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("app.jwt.secret", () -> "dGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtdGVzdGUtMzJi");
        registry.add("app.crypto.key", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    }

    @Value("${local.server.port}")
    protected int port;

    protected RestClient http;

    @BeforeEach
    void setUpClient() {
        http = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> { })
            .build();
    }

    /** Cria um usuario novo e devolve o token. */
    protected String newUserToken() {
        String email = "user-" + UUID.randomUUID() + "@test.com";
        Map<?, ?> body = http.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Teste", "email", email, "password", "senha-forte-123"))
            .retrieve().body(Map.class);
        return (String) body.get("accessToken");
    }

    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map<String, Object>> post(String token, String uri, Object body) {
        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) http.post().uri(uri)
            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .body(body).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map<String, Object>> put(String token, String uri, Object body) {
        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) http.put().uri(uri)
            .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .body(body).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("unchecked")
    protected ResponseEntity<Map<String, Object>> get(String token, String uri) {
        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) http.get().uri(uri)
            .header("Authorization", "Bearer " + token).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getList(String token, String uri) {
        return http.get().uri(uri).header("Authorization", "Bearer " + token).retrieve().body(List.class);
    }

    protected int delete(String token, String uri) {
        return http.delete().uri(uri).header("Authorization", "Bearer " + token)
            .retrieve().toBodilessEntity().getStatusCode().value();
    }

    /** Numeros do JSON chegam como Double/Integer no cliente; compara pelo valor. */
    protected static java.math.BigDecimal num(Object value) {
        return new java.math.BigDecimal(value.toString());
    }

    protected Long createCategory(String token, String name, String type) {
        return ((Number) post(token, "/api/categories", Map.of("name", name, "type", type)).getBody().get("id")).longValue();
    }

    protected Long createTransaction(String token, Long categoryId, String amount, String date) {
        return ((Number) post(token, "/api/transactions",
            Map.of("categoryId", categoryId, "amount", amount, "occurredOn", date)).getBody().get("id")).longValue();
    }
}
