package com.fabrodiego.moneyguard.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fabrodiego.moneyguard.AbstractIntegrationTest;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerLoginAndMe() {
        var register = http.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Diego", "email", "Diego@Test.com", "password", "senha-forte-123"))
            .retrieve().toEntity(Map.class);
        assertThat(register.getStatusCode().value()).isEqualTo(201);

        var duplicate = http.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("name", "Outro", "email", "diego@test.com", "password", "senha-forte-123"))
            .retrieve().toBodilessEntity();
        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);

        var badLogin = http.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", "diego@test.com", "password", "errada123"))
            .retrieve().toBodilessEntity();
        assertThat(badLogin.getStatusCode().value()).isEqualTo(401);

        var login = http.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("email", "diego@test.com", "password", "senha-forte-123"))
            .retrieve().toEntity(Map.class);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        String token = (String) login.getBody().get("accessToken");

        var me = get(token, "/api/auth/me");
        assertThat(me.getStatusCode().value()).isEqualTo(200);
        assertThat(me.getBody().get("email")).isEqualTo("diego@test.com");
    }

    @Test
    void protectedRouteWithoutTokenIs401() {
        var me = http.get().uri("/api/auth/me").retrieve().toBodilessEntity();
        assertThat(me.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void healthIsPublic() {
        var health = http.get().uri("/actuator/health").retrieve().toBodilessEntity();
        assertThat(health.getStatusCode().value()).isEqualTo(200);
    }
}
