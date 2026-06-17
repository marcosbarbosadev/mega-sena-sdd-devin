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

class SincronizacaoLacunaIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private SincronizacaoService sincronizacaoService;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Test
    void fillsGapBetweenStoredAndLatest() {
        // Pre-insert contest 5000
        Concurso existing = new Concurso();
        existing.setNumero(5000);
        existing.setDataSorteio(LocalDate.of(2024, 1, 1));
        existing.setValorPremio(BigDecimal.valueOf(1000000));
        existing.addDezena(1);
        existing.addDezena(2);
        existing.addDezena(3);
        existing.addDezena(4);
        existing.addDezena(5);
        existing.addDezena(6);
        concursoRepository.save(existing);

        // Latest is 5003 -> should import 5001, 5002, 5003
        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(5003, "04/01/2024",
                                new String[]{"10", "20", "30", "40", "50", "60"}, 5000000.00))));

        wireMock.stubFor(get(urlEqualTo("/5001"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(5001, "02/01/2024",
                                new String[]{"07", "14", "21", "28", "35", "42"}, 2000000.00))));

        wireMock.stubFor(get(urlEqualTo("/5002"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(concursoPayload(5002, "03/01/2024",
                                new String[]{"08", "16", "24", "32", "48", "56"}, 3000000.00))));

        SyncRun run = sincronizacaoService.sincronizar(OrigemSync.AGENDADA);

        assertEquals(StatusSync.SUCESSO, run.getStatus());
        assertEquals(3, run.getConcursosImportados());

        assertTrue(concursoRepository.existsByNumero(5001));
        assertTrue(concursoRepository.existsByNumero(5002));
        assertTrue(concursoRepository.existsByNumero(5003));
    }
}
