package com.megasena.sync.fonte;

import com.megasena.sync.concurso.Concurso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConcursoMapperTest {

    private ConcursoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ConcursoMapper();
    }

    @Test
    void mapValidResponse() {
        CaixaConcursoResponse response = ConcursoValidatorTest.buildValid();

        Concurso result = mapper.toConcurso(response);

        assertEquals(2700, result.getNumero());
        assertEquals(LocalDate.of(2024, 5, 29), result.getDataSorteio());
        assertEquals(0, BigDecimal.valueOf(52000000.00).compareTo(result.getValorPremio()));
        assertEquals(6, result.getDezenas().size());

        List<Integer> dezenas = result.getDezenas().stream()
                .map(d -> d.getDezena())
                .sorted()
                .toList();
        assertEquals(List.of(4, 17, 23, 38, 51, 60), dezenas);
    }

    @Test
    void parseDateCorrectly() {
        assertEquals(LocalDate.of(2024, 1, 15), mapper.parseDate("15/01/2024"));
        assertEquals(LocalDate.of(2023, 12, 31), mapper.parseDate("31/12/2023"));
    }

    @Test
    void extractSenaPrize() {
        CaixaConcursoResponse response = ConcursoValidatorTest.buildValid();
        BigDecimal prize = mapper.extractSenaPrize(response);
        assertNotNull(prize);
        assertEquals(0, BigDecimal.valueOf(52000000.00).compareTo(prize));
    }

    @Test
    void extractSenaPrizeFromMultipleFaixas() {
        CaixaConcursoResponse response = new CaixaConcursoResponse();
        CaixaConcursoResponse.RateioPremio quina = new CaixaConcursoResponse.RateioPremio();
        quina.setDescricaoFaixa("Quina");
        quina.setValorPremio(BigDecimal.valueOf(50000));
        CaixaConcursoResponse.RateioPremio sena = new CaixaConcursoResponse.RateioPremio();
        sena.setDescricaoFaixa("Sena");
        sena.setValorPremio(BigDecimal.valueOf(99000000));
        response.setListaRateioPremio(List.of(quina, sena));

        assertEquals(0, BigDecimal.valueOf(99000000).compareTo(mapper.extractSenaPrize(response)));
    }
}
