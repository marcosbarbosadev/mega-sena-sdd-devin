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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SincronizacaoNovoConcursoIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private SincronizacaoService sincronizacaoService;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Test
    void newContestFromSourceIsStoredLocally() {
        // Pre-insert contest so sync only needs to fetch the "new" one
        Concurso existing = new Concurso();
        existing.setNumero(2699);
        existing.setDataSorteio(LocalDate.of(2024, 5, 28));
        existing.setValorPremio(BigDecimal.valueOf(40000000));
        existing.addDezena(1); existing.addDezena(2); existing.addDezena(3);
        existing.addDezena(4); existing.addDezena(5); existing.addDezena(6);
        concursoRepository.save(existing);

        String payload = concursoPayload(2700, "29/05/2024",
                new String[]{"04", "17", "23", "38", "51", "60"}, 52000000.00);

        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(payload)));

        SyncRun run = sincronizacaoService.sincronizar(OrigemSync.AGENDADA);

        assertEquals(StatusSync.SUCESSO, run.getStatus());
        assertEquals(1, run.getConcursosImportados());

        var concurso = concursoRepository.findByNumero(2700);
        assertTrue(concurso.isPresent());
        assertNotNull(concurso.get().getDataSorteio());
        assertEquals(6, concurso.get().getDezenas().size());
    }
}
