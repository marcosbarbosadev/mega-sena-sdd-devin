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

class AdminBootstrapIT extends AbstractIntegrationTest {

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
    void emailBootstrapNasceAtivoAdministrador() {
        verificadorFake.registrarToken("token-admin-boot", "uid-admin-boot", "admin@bootstrap.com", true, MetodoLogin.SENHA);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer token-admin-boot");
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("ATIVO"));
        assertTrue(response.getBody().contains("ADMINISTRADOR"));

        var usuario = usuarioRepository.findByProviderUid("uid-admin-boot");
        assertTrue(usuario.isPresent());
        assertEquals(EstadoConta.ATIVO, usuario.get().getEstado());
        assertEquals(Papel.ADMINISTRADOR, usuario.get().getPapel());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
