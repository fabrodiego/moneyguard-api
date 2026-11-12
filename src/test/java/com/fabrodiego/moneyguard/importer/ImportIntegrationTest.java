package com.fabrodiego.moneyguard.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;

import com.fabrodiego.moneyguard.AbstractIntegrationTest;

class ImportIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> upload(String token, byte[] content, String filename) {
        var parts = new LinkedMultiValueMap<String, Object>();
        parts.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) http.post().uri("/api/imports/preview")
            .header("Authorization", "Bearer " + token).contentType(MediaType.MULTIPART_FORM_DATA)
            .body(parts).retrieve().toEntity(Map.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(ResponseEntity<Map<String, Object>> res) {
        return (List<Map<String, Object>>) res.getBody().get("items");
    }

    private static Map<String, Object> approval(Map<String, Object> item, Long categoryId, Long merchantId) {
        Map<String, Object> body = new HashMap<>();
        body.put("importKey", item.get("importKey"));
        body.put("type", item.get("type"));
        body.put("categoryId", categoryId);
        body.put("merchantId", merchantId);
        body.put("amount", item.get("amount").toString());
        body.put("description", item.get("suggestedDescription"));
        body.put("occurredOn", item.get("occurredOn"));
        body.put("alias", item.get("description"));
        return body;
    }

    @Test
    void statementPreviewApproveLearnsAliasAndBlocksDuplicates() {
        String token = newUserToken(); // usuario "Teste"
        Long food = createCategory(token, "Alimentação", "EXPENSE");
        Long income = createCategory(token, "Rendimentos", "INCOME");
        Long bakery = ((Number) post(token, "/api/merchants", Map.of("name", "Padaria do Bairro", "categoryId", food))
            .getBody().get("id")).longValue();

        var preview = upload(token, InterFixtures.statementBytes(), "extrato.csv");
        assertThat(preview.getStatusCode().value()).isEqualTo(200);
        assertThat(preview.getBody().get("kind")).isEqualTo("STATEMENT");
        assertThat((List<?>) preview.getBody().get("ignored")).hasSize(1);
        List<Map<String, Object>> items = items(preview);
        assertThat(items).hasSize(7);
        assertThat(items).noneMatch(i -> (Boolean) i.get("alreadyImported"));

        // Pix recebido de "Teste Da Silva" = transferencia propria
        assertThat(items.get(4).get("ownTransfer")).isEqualTo(true);
        assertThat(items.get(0).get("ownTransfer")).isEqualTo(false);
        // duas compras identicas em dias diferentes / iguais nao colidem
        assertThat(items.get(2).get("importKey")).isNotEqualTo(items.get(3).get("importKey"));
        assertThat(items.get(2).get("suggestion")).isNull();

        // aprova a compra na padaria com a empresa: aprende o apelido
        var approved = post(token, "/api/imports/approve", approval(items.get(2), food, bakery));
        assertThat(approved.getStatusCode().value()).isEqualTo(201);
        assertThat(approved.getBody().get("source")).isEqualTo("IMPORT");
        assertThat(approved.getBody().get("type")).isEqualTo("EXPENSE");

        // aprovar de novo a mesma linha = 409
        assertThat(post(token, "/api/imports/approve", approval(items.get(2), food, bakery)).getStatusCode().value())
            .isEqualTo(409);
        // categoria de renda para um gasto = 400
        assertThat(post(token, "/api/imports/approve", approval(items.get(0), income, null)).getStatusCode().value())
            .isEqualTo(400);

        // reimportar: a aprovada aparece como ja importada; a pulada volta normal; apelido sugere a empresa
        List<Map<String, Object>> again = items(upload(token, InterFixtures.statementBytes(), "extrato.csv"));
        assertThat(again.get(2).get("alreadyImported")).isEqualTo(true);
        assertThat(again.get(0).get("alreadyImported")).isEqualTo(false);
        assertThat(again.get(3).get("alreadyImported")).isEqualTo(false);
        assertThat(((Map<?, ?>) again.get(3).get("suggestion")).get("name")).isEqualTo("Padaria do Bairro");

        // chave guardada e HMAC, nao o texto
        String key = jdbc.queryForObject("select import_key from moneyguard.transactions where id = ?", String.class,
            ((Number) approved.getBody().get("id")).longValue());
        assertThat(key).matches("[0-9a-f]{64}");
    }

    @Test
    void billPreviewSuggestsByMerchantNameAndWarnsOnTotal() throws Exception {
        String token = newUserToken();
        Long transport = createCategory(token, "Transporte", "EXPENSE");
        post(token, "/api/merchants", Map.of("name", "Uber", "categoryId", transport));

        var preview = upload(token, InterFixtures.billPdf(1024), "fatura.pdf");
        assertThat(preview.getStatusCode().value()).isEqualTo(200);
        assertThat(preview.getBody().get("kind")).isEqualTo("CARD_BILL");
        List<Map<String, Object>> items = items(preview);
        assertThat(items).hasSize(8);
        assertThat(items.get(0).get("installment")).isEqualTo("2/2");
        assertThat(items.get(0).get("suggestedDescription")).isEqualTo("LOJA EXEMPLO CENTRO (2/2)");
        assertThat(((Map<?, ?>) items.get(2).get("suggestion")).get("name")).isEqualTo("Uber");
        // fixture: total impresso 516,40 x lido 536,40
        assertThat((String) preview.getBody().get("warning")).contains("nao bate");
    }

    @Test
    void rejectsUnknownFiles() {
        String token = newUserToken();
        assertThat(upload(token, "qualquer coisa".getBytes(), "x.csv").getStatusCode().value()).isEqualTo(400);
        assertThat(upload(token, "%PDF-1.7 quebrado".getBytes(), "x.pdf").getStatusCode().value()).isEqualTo(400);
    }
}
