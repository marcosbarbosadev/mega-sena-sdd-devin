package com.megasena.sync.sincronizacao;

import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.support.AbstractWireMockIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
        String payload = concursoPayload(2700, "29/05/2024",
                new String[]{"04", "17", "23", "38", "51", "60"}, 52000000.00);

        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(payload)));

        SyncRun run = sincronizacaoService.sincronizar(OrigemSync.AGENDADA);

        assertEquals(StatusSync.SUCESSO, run.getStatus());
        assertTrue(run.getConcursosImportados() > 0);

        var concurso = concursoRepository.findByNumero(2700);
        assertTrue(concurso.isPresent());
        assertNotNull(concurso.get().getDataSorteio());
        assertEquals(6, concurso.get().getDezenas().size());
    }
}
