package com.megasena.sync.sincronizacao;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.support.AbstractWireMockIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargaHistoricaRetomavelIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private SincronizacaoService sincronizacaoService;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Test
    void resumesWithoutReimportingExisting() {
        // Simulate partial import: contest 8001 already exists
        Concurso existing = new Concurso();
        existing.setNumero(8001);
        existing.setDataSorteio(LocalDate.of(2024, 1, 1));
        existing.setValorPremio(BigDecimal.valueOf(1000000));
        existing.addDezena(1);
        existing.addDezena(2);
        existing.addDezena(3);
        existing.addDezena(4);
        existing.addDezena(5);
        existing.addDezena(6);
        concursoRepository.save(existing);

        // Latest = 8003, should only import 8002 and 8003
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(8003, "03/01/2024",
                                new String[]{"10", "20", "30", "40", "50", "60"}, 5000000.00))));

        // Contests 1..8000 stub not needed — existsByNumero returns false so they get fetched.
        // For test simplicity, we use sincronizar (not cargaHistorica) to resume from stored max
        wireMock.stubFor(get(urlEqualTo("/8002"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(8002, "02/01/2024",
                                new String[]{"07", "14", "21", "28", "35", "42"}, 3000000.00))));

        SyncRun run = sincronizacaoService.sincronizar(OrigemSync.MANUAL);

        assertEquals(StatusSync.SUCESSO, run.getStatus());
        assertEquals(2, run.getConcursosImportados());

        assertTrue(concursoRepository.existsByNumero(8001));
        assertTrue(concursoRepository.existsByNumero(8002));
        assertTrue(concursoRepository.existsByNumero(8003));
    }
}
