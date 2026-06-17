package com.megasena.sync.sincronizacao;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.support.AbstractWireMockIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DegradacaoGraciosaIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private SincronizacaoService sincronizacaoService;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void existingDataServedWhenSourceUnavailable() {
        // Pre-insert a contest
        Concurso existing = new Concurso();
        existing.setNumero(6000);
        existing.setDataSorteio(LocalDate.of(2024, 3, 1));
        existing.setValorPremio(BigDecimal.valueOf(8000000));
        existing.addDezena(5);
        existing.addDezena(15);
        existing.addDezena(25);
        existing.addDezena(35);
        existing.addDezena(45);
        existing.addDezena(55);
        concursoRepository.save(existing);

        // Source unavailable
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(500)));

        SyncRun run = sincronizacaoService.sincronizar(OrigemSync.AGENDADA);
        assertEquals(StatusSync.FALHA, run.getStatus());

        // Existing data still accessible
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/concursos/6000", String.class);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}
