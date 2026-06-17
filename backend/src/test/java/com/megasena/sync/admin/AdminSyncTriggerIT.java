package com.megasena.sync.admin;

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

class AdminSyncTriggerIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void manualSyncReturns202() {
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(9100, "10/06/2024",
                                new String[]{"03", "13", "23", "33", "43", "53"}, 8000000.00))));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-admin-token");

        var response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/sync/run",
                HttpMethod.POST, new HttpEntity<>(headers), SyncRunResponse.class);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("MANUAL", response.getBody().origem());
    }

    @Test
    void manualSyncReturns401WithoutToken() {
        var response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/sync/run",
                HttpMethod.POST, new HttpEntity<>(null), String.class);

        assertEquals(401, response.getStatusCode().value());
    }
}
