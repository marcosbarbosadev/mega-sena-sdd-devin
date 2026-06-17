package com.megasena.sync.admin;

import com.megasena.sync.sincronizacao.OrigemSync;
import com.megasena.sync.sincronizacao.SincronizacaoService;
import com.megasena.sync.support.AbstractWireMockIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSyncStatusIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private SincronizacaoService sincronizacaoService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void statusReturns200AfterSync() {
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(9000, "01/06/2024",
                                new String[]{"05", "15", "25", "35", "45", "55"}, 10000000.00))));

        sincronizacaoService.sincronizar(OrigemSync.AGENDADA);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-admin-token");

        var response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/sync/status",
                HttpMethod.GET, new HttpEntity<>(headers), SyncRunResponse.class);

        assertEquals(200, response.getStatusCode().value());
        SyncRunResponse body = response.getBody();
        assertEquals("SUCESSO", body.status());
        assertTrue(body.concursosImportados() > 0);
    }

    @Test
    void statusReturns401WithoutToken() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/admin/sync/status", String.class);
        assertEquals(401, response.getStatusCode().value());
    }
}
