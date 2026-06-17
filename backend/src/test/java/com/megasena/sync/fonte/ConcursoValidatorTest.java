package com.megasena.sync.fonte;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcursoValidatorTest {

    private ConcursoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConcursoValidator();
    }

    @Test
    void validResponse() {
        assertTrue(validator.isValid(buildValid()));
    }

    @Test
    void rejectNullNumero() {
        CaixaConcursoResponse r = buildValid();
        r.setNumero(null);
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectZeroNumero() {
        CaixaConcursoResponse r = buildValid();
        r.setNumero(0);
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectNegativeNumero() {
        CaixaConcursoResponse r = buildValid();
        r.setNumero(-1);
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectNullDataApuracao() {
        CaixaConcursoResponse r = buildValid();
        r.setDataApuracao(null);
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectBlankDataApuracao() {
        CaixaConcursoResponse r = buildValid();
        r.setDataApuracao("  ");
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectNullDezenas() {
        CaixaConcursoResponse r = buildValid();
        r.setListaDezenas(null);
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectFewerThan6Dezenas() {
        CaixaConcursoResponse r = buildValid();
        r.setListaDezenas(List.of("01", "02", "03", "04", "05"));
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectMoreThan6Dezenas() {
        CaixaConcursoResponse r = buildValid();
        r.setListaDezenas(List.of("01", "02", "03", "04", "05", "06", "07"));
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectDuplicateDezenas() {
        CaixaConcursoResponse r = buildValid();
        r.setListaDezenas(List.of("01", "01", "03", "04", "05", "06"));
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectDezenaOutOfRange() {
        CaixaConcursoResponse r = buildValid();
        r.setListaDezenas(List.of("00", "02", "03", "04", "05", "06"));
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectDezenaAbove60() {
        CaixaConcursoResponse r = buildValid();
        r.setListaDezenas(List.of("01", "02", "03", "04", "05", "61"));
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectNonNumericDezena() {
        CaixaConcursoResponse r = buildValid();
        r.setListaDezenas(List.of("01", "02", "03", "04", "05", "AB"));
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectMissingSenaFaixa() {
        CaixaConcursoResponse r = buildValid();
        CaixaConcursoResponse.RateioPremio rp = new CaixaConcursoResponse.RateioPremio();
        rp.setDescricaoFaixa("Quina");
        rp.setValorPremio(BigDecimal.valueOf(1000));
        r.setListaRateioPremio(List.of(rp));
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectNegativeSenaPrize() {
        CaixaConcursoResponse r = buildValid();
        CaixaConcursoResponse.RateioPremio rp = new CaixaConcursoResponse.RateioPremio();
        rp.setDescricaoFaixa("Sena");
        rp.setValorPremio(BigDecimal.valueOf(-1));
        r.setListaRateioPremio(List.of(rp));
        assertFalse(validator.isValid(r));
    }

    @Test
    void rejectNullRateioPremio() {
        CaixaConcursoResponse r = buildValid();
        r.setListaRateioPremio(null);
        assertFalse(validator.isValid(r));
    }

    @Test
    void acceptZeroPrize() {
        CaixaConcursoResponse r = buildValid();
        r.getListaRateioPremio().get(0).setValorPremio(BigDecimal.ZERO);
        assertTrue(validator.isValid(r));
    }

    static CaixaConcursoResponse buildValid() {
        CaixaConcursoResponse r = new CaixaConcursoResponse();
        r.setNumero(2700);
        r.setDataApuracao("29/05/2024");
        r.setListaDezenas(List.of("04", "17", "23", "38", "51", "60"));

        CaixaConcursoResponse.RateioPremio rp = new CaixaConcursoResponse.RateioPremio();
        rp.setDescricaoFaixa("Sena");
        rp.setValorPremio(BigDecimal.valueOf(52000000.00));
        r.setListaRateioPremio(List.of(rp));

        return r;
    }
}
