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

class SincronizacaoIdempotenciaIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private SincronizacaoService sincronizacaoService;

    @Autowired
    private ConcursoRepository concursoRepository;

    @Test
    void runSyncTwiceNoDuplicates() {
        // Pre-insert so sync only needs to fetch contest 3000
        Concurso existing = new Concurso();
        existing.setNumero(2999);
        existing.setDataSorteio(LocalDate.of(2024, 6, 14));
        existing.setValorPremio(BigDecimal.valueOf(5000000));
        existing.addDezena(7); existing.addDezena(14); existing.addDezena(21);
        existing.addDezena(28); existing.addDezena(35); existing.addDezena(42);
        concursoRepository.save(existing);

        String payload = concursoPayload(3000, "15/06/2024",
                new String[]{"01", "12", "25", "33", "44", "58"}, 10000000.00);

        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(payload)));

        sincronizacaoService.sincronizar(OrigemSync.AGENDADA);

        Concurso first = concursoRepository.findByNumero(3000).orElseThrow();
        BigDecimal originalPrize = first.getValorPremio();
        int originalDezenaCount = first.getDezenas().size();

        // Run again with same data — should be idempotent
        SyncRun secondRun = sincronizacaoService.sincronizar(OrigemSync.AGENDADA);

        assertEquals(StatusSync.SUCESSO, secondRun.getStatus());
        assertEquals(0, secondRun.getConcursosImportados());

        long count = concursoRepository.findAll().stream()
                .filter(c -> c.getNumero() == 3000)
                .count();
        assertEquals(1, count);

        Concurso after = concursoRepository.findByNumero(3000).orElseThrow();
        assertEquals(0, originalPrize.compareTo(after.getValorPremio()));
        assertEquals(originalDezenaCount, after.getDezenas().size());
    }
}
