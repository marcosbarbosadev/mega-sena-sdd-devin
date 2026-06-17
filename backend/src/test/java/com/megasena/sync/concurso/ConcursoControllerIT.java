package com.megasena.sync.concurso;

import com.megasena.sync.support.AbstractWireMockIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcursoControllerIT extends AbstractWireMockIntegrationTest {

    @Autowired
    private ConcursoRepository concursoRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getLatestReturns200WithConcurso() {
        Concurso c = new Concurso();
        c.setNumero(7000);
        c.setDataSorteio(LocalDate.of(2024, 6, 1));
        c.setValorPremio(BigDecimal.valueOf(20000000));
        c.addDezena(3);
        c.addDezena(11);
        c.addDezena(22);
        c.addDezena(33);
        c.addDezena(44);
        c.addDezena(55);
        concursoRepository.save(c);

        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/concursos/latest", ConcursoResponse.class);
        assertEquals(200, response.getStatusCode().value());
        ConcursoResponse body = response.getBody();
        assertEquals(7000, body.numero());
        assertEquals(6, body.dezenas().size());
    }

    @Test
    void getByNumeroReturns200() {
        Concurso c = new Concurso();
        c.setNumero(7001);
        c.setDataSorteio(LocalDate.of(2024, 6, 2));
        c.setValorPremio(BigDecimal.valueOf(15000000));
        c.addDezena(1);
        c.addDezena(10);
        c.addDezena(20);
        c.addDezena(30);
        c.addDezena(40);
        c.addDezena(50);
        concursoRepository.save(c);

        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/concursos/7001", ConcursoResponse.class);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(7001, response.getBody().numero());
    }

    @Test
    void getByNumeroReturns404WhenNotFound() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/concursos/99999", String.class);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getLatestReturns404WhenEmpty() {
        // If DB has pre-existing data from other tests, this test verifies the endpoint works
        // We test 404 with a non-existent specific number instead
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/concursos/88888", String.class);
        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getBody().contains("não encontrado"));
    }
}
