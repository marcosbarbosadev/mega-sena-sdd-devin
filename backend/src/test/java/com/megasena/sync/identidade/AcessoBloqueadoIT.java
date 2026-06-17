package com.megasena.sync.identidade;

import com.megasena.sync.support.AbstractIntegrationTest;
import com.megasena.sync.support.VerificadorDeIdentidadeFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class AcessoBloqueadoIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VerificadorDeIdentidadeFake verificadorFake;

    @BeforeEach
    void setUp() {
        verificadorFake.limpar();
    }

    @Test
    void contaPendenteBarradaEmPerfil() {
        verificadorFake.registrarToken("token-pend", "uid-pend", "pendente@teste.com", true, MetodoLogin.SENHA);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer token-pend");
        // First call provisions the account in PENDENTE
        restTemplate.exchange(url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Try to access protected resource
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/perfil"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("CONTA_PENDENTE"));
    }

    @Test
    void semTokenRetorna401() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void tokenInvalidoRetorna401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid-token");
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(401, response.getStatusCode().value());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
