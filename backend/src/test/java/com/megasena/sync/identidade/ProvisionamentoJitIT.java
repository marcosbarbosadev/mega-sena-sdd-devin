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

class ProvisionamentoJitIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VerificadorDeIdentidadeFake verificadorFake;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EventoIdentidadeRepository eventoRepository;

    @BeforeEach
    void setUp() {
        verificadorFake.limpar();
    }

    @Test
    void primeiraChamadaCriaContaPendente() {
        verificadorFake.registrarToken("token-novo", "uid-novo", "novo@teste.com", true, MetodoLogin.SENHA);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer token-novo");
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("PENDENTE"));
        assertTrue(response.getBody().contains("USUARIO"));

        var usuario = usuarioRepository.findByProviderUid("uid-novo");
        assertTrue(usuario.isPresent());
        assertEquals(EstadoConta.PENDENTE, usuario.get().getEstado());
        assertEquals(Papel.USUARIO, usuario.get().getPapel());
    }

    @Test
    void segundaChamadaNaoDuplicaConta() {
        verificadorFake.registrarToken("token-dup", "uid-dup", "dup@teste.com", true, MetodoLogin.SENHA);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer token-dup");
        restTemplate.exchange(url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        restTemplate.exchange(url("/api/auth/me"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        long count = usuarioRepository.findAll().stream()
                .filter(u -> u.getProviderUid().equals("uid-dup")).count();
        assertEquals(1, count);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
