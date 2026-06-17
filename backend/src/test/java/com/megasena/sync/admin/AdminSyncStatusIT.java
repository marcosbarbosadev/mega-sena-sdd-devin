package com.megasena.sync.admin;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.identidade.MetodoLogin;
import com.megasena.sync.sincronizacao.OrigemSync;
import com.megasena.sync.sincronizacao.SincronizacaoService;
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
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSyncStatusIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private SincronizacaoService sincronizacaoService;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VerificadorDeIdentidadeFake verificadorFake;

    @BeforeEach
    void setUpAuth() {
        verificadorFake.limpar();
        verificadorFake.registrarToken("admin-token", "uid-admin-sync", "admin@bootstrap.com", true, MetodoLogin.SENHA);
        // Provision admin account
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth("admin-token");
        restTemplate.exchange("http://localhost:" + port + "/api/auth/me",
                HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    @Test
    void statusReturns200AfterSync() {
        Concurso existing = new Concurso();
        existing.setNumero(8999);
        existing.setDataSorteio(LocalDate.of(2024, 5, 31));
        existing.setValorPremio(BigDecimal.valueOf(5000000));
        existing.addDezena(1); existing.addDezena(2); existing.addDezena(3);
        existing.addDezena(4); existing.addDezena(5); existing.addDezena(6);
        concursoRepository.save(existing);

        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(9000, "01/06/2024",
                                new String[]{"05", "15", "25", "35", "45", "55"}, 10000000.00))));

        sincronizacaoService.sincronizar(OrigemSync.AGENDADA);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("admin-token");

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
