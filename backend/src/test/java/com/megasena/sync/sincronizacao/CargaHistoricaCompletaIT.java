package com.megasena.sync.sincronizacao;

import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.support.AbstractWireMockIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargaHistoricaCompletaIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private SincronizacaoService sincronizacaoService;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Test
    void emptyDatabaseImportsAllContests() {
        // Set up 3 contests (1, 2, 3)
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(3, "03/01/2024",
                                new String[]{"10", "20", "30", "40", "50", "60"}, 3000000.00))));

        wireMock.stubFor(get(urlEqualTo("/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(1, "01/01/2024",
                                new String[]{"01", "11", "21", "31", "41", "51"}, 1000000.00))));

        wireMock.stubFor(get(urlEqualTo("/2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(2, "02/01/2024",
                                new String[]{"02", "12", "22", "32", "42", "52"}, 2000000.00))));

        SyncRun run = sincronizacaoService.cargaHistorica();

        assertEquals(StatusSync.SUCESSO, run.getStatus());
        assertEquals(3, run.getConcursosImportados());

        assertTrue(concursoRepository.existsByNumero(1));
        assertTrue(concursoRepository.existsByNumero(2));
        assertTrue(concursoRepository.existsByNumero(3));
    }
}
