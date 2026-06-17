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

class EmailNaoVerificadoIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VerificadorDeIdentidadeFake verificadorFake;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        verificadorFake.limpar();
    }

    @Test
    void googleComEmailNaoVerificadoRetorna403() {
        verificadorFake.registrarToken("token-nao-verificado", "uid-nv", "nv@teste.com", false, MetodoLogin.GOOGLE);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer token-nao-verificado");
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody().contains("EMAIL_NAO_VERIFICADO"));

        assertFalse(usuarioRepository.findByProviderUid("uid-nv").isPresent());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
