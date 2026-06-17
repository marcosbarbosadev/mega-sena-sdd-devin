package com.megasena.sync.admin;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.identidade.MetodoLogin;
import com.megasena.sync.support.AbstractWireMockIntegrationTest;
import com.megasena.sync.support.VerificadorDeIdentidadeFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminSyncTriggerIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private VerificadorDeIdentidadeFake verificadorFake;

    @BeforeEach
    void setUpAuth() {
        verificadorFake.limpar();
        verificadorFake.registrarToken("admin-token", "uid-admin-trigger", "admin@bootstrap.com", true, MetodoLogin.SENHA);
        // Provision admin account
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth("admin-token");
        restTemplate.exchange("http://localhost:" + port + "/api/auth/me",
                HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    @Test
    void manualSyncReturns202() {
        Concurso existing = new Concurso();
        existing.setNumero(9099);
        existing.setDataSorteio(LocalDate.of(2024, 6, 9));
        existing.setValorPremio(BigDecimal.valueOf(4000000));
        existing.addDezena(1); existing.addDezena(2); existing.addDezena(3);
        existing.addDezena(4); existing.addDezena(5); existing.addDezena(6);
        concursoRepository.save(existing);

        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(9100, "10/06/2024",
                                new String[]{"03", "13", "23", "33", "43", "53"}, 8000000.00))));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("admin-token");

        var response = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/sync/run",
                HttpMethod.POST, new HttpEntity<>(headers), SyncRunResponse.class);

        assertEquals(202, response.getStatusCode().value());
        assertEquals("MANUAL", response.getBody().origem());
    }

    @Test
    void manualSyncReturns401WithoutToken() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/admin/sync/run"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }
}
