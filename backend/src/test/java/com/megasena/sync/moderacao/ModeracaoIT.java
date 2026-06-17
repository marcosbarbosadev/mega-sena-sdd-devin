package com.megasena.sync.moderacao;

import com.megasena.sync.identidade.EstadoConta;
import com.megasena.sync.identidade.EventoIdentidadeRepository;
import com.megasena.sync.identidade.MetodoLogin;
import com.megasena.sync.identidade.UsuarioRepository;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class ModeracaoIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VerificadorDeIdentidadeFake verificadorFake;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DecisaoModeracaoRepository decisaoRepository;

    @Autowired
    private EventoIdentidadeRepository eventoIdentidadeRepository;

    @BeforeEach
    void setUp() {
        verificadorFake.limpar();
        decisaoRepository.deleteAll();
        eventoIdentidadeRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void listarPendentesRetornaContasPendentes() {
        provisionarAdmin();
        provisionarUsuario("user-token", "uid-user", "user@teste.com");
        provisionarUsuario("user2-token", "uid-user2", "user2@teste.com");

        ResponseEntity<String> response = get("/api/admin/contas/pendentes", "admin-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("user@teste.com"));
        assertTrue(response.getBody().contains("user2@teste.com"));
    }

    @Test
    void aprovarContaMudaEstadoParaAtivo() {
        provisionarAdmin();
        provisionarUsuario("user-token", "uid-user", "user@teste.com");

        var usuario = usuarioRepository.findByProviderUid("uid-user").orElseThrow();
        assertEquals(EstadoConta.PENDENTE, usuario.getEstado());

        ResponseEntity<String> response = post(
                "/api/admin/contas/" + usuario.getId() + "/aprovar", "admin-token", null);
        assertEquals(204, response.getStatusCode().value());

        var atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(EstadoConta.ATIVO, atualizado.getEstado());
    }

    @Test
    void reprovarContaMudaEstadoParaReprovado() {
        provisionarAdmin();
        provisionarUsuario("user2-token", "uid-user2", "user2@teste.com");

        var usuario = usuarioRepository.findByProviderUid("uid-user2").orElseThrow();

        ResponseEntity<String> response = post(
                "/api/admin/contas/" + usuario.getId() + "/reprovar", "admin-token",
                "{\"motivo\":\"Dados inválidos\"}");
        assertEquals(204, response.getStatusCode().value());

        var atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertEquals(EstadoConta.REPROVADO, atualizado.getEstado());
    }

    @Test
    void reprovarSemMotivoRetorna400() {
        provisionarAdmin();
        provisionarUsuario("user-token", "uid-user", "user@teste.com");

        var usuario = usuarioRepository.findByProviderUid("uid-user").orElseThrow();

        ResponseEntity<String> response = post(
                "/api/admin/contas/" + usuario.getId() + "/reprovar", "admin-token",
                "{\"motivo\":\"\"}");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void aprovarContaJaAprovadaRetorna409() {
        provisionarAdmin();
        provisionarUsuario("user-token", "uid-user", "user@teste.com");

        var usuario = usuarioRepository.findByProviderUid("uid-user").orElseThrow();

        post("/api/admin/contas/" + usuario.getId() + "/aprovar", "admin-token", null);
        ResponseEntity<String> response = post(
                "/api/admin/contas/" + usuario.getId() + "/aprovar", "admin-token", null);
        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void usuarioNaoAdminBarradoDePendentes() {
        provisionarAdmin();
        provisionarUsuario("user-token", "uid-user", "user@teste.com");

        ResponseEntity<String> response = get("/api/admin/contas/pendentes", "user-token");
        assertEquals(403, response.getStatusCode().value());
    }

    private void provisionarAdmin() {
        verificadorFake.registrarToken("admin-token", "uid-admin", "admin@bootstrap.com", true, MetodoLogin.SENHA);
        get("/api/auth/me", "admin-token");
    }

    private void provisionarUsuario(String token, String uid, String email) {
        verificadorFake.registrarToken(token, uid, email, true, MetodoLogin.SENHA);
        get("/api/auth/me", token);
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> post(String path, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = body != null
                ? new HttpEntity<>(body, headers)
                : new HttpEntity<>(headers);
        return restTemplate.exchange(url(path), HttpMethod.POST, entity, String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
