package com.megasena.sync.jogo;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.identidade.EstadoConta;
import com.megasena.sync.identidade.EventoIdentidadeRepository;
import com.megasena.sync.identidade.MetodoLogin;
import com.megasena.sync.identidade.Papel;
import com.megasena.sync.identidade.Usuario;
import com.megasena.sync.identidade.UsuarioRepository;
import com.megasena.sync.moderacao.DecisaoModeracaoRepository;
import com.megasena.sync.support.AbstractIntegrationTest;
import com.megasena.sync.support.FonteAleatoriedadeFake;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JogoIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VerificadorDeIdentidadeFake verificadorFake;

    @Autowired
    private FonteAleatoriedadeFake fonteAleatoriedadeFake;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JogoRepository jogoRepository;

    @Autowired
    private EventoJogoRepository eventoJogoRepository;

    @Autowired
    private EventoIdentidadeRepository eventoIdentidadeRepository;

    @Autowired
    private DecisaoModeracaoRepository decisaoModeracaoRepository;

    @Autowired
    private ConcursoRepository concursoRepository;

    private UUID usuarioAtivoId;

    @BeforeEach
    void setUp() {
        verificadorFake.limpar();
        eventoJogoRepository.deleteAll();
        jogoRepository.deleteAll();
        decisaoModeracaoRepository.deleteAll();
        eventoIdentidadeRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario ativo = new Usuario();
        ativo.setId(UUID.randomUUID());
        ativo.setProviderUid("uid-ativo");
        ativo.setEmail("ativo@teste.com");
        ativo.setEstado(EstadoConta.ATIVO);
        ativo.setPapel(Papel.USUARIO);
        ativo.setMetodoLogin(MetodoLogin.SENHA);
        ativo = usuarioRepository.save(ativo);
        usuarioAtivoId = ativo.getId();

        verificadorFake.registrarToken("ativo-token", "uid-ativo", "ativo@teste.com", true, MetodoLogin.SENHA);
    }

    @Test
    void criarJogoManualComSeisDezenasRetorna201() {
        String body = """
                {"dezenas":[5,10,15,20,25,30]}
                """;
        ResponseEntity<String> response = post("/api/jogos", "ativo-token", body);
        assertEquals(201, response.getStatusCode().value());
        assertTrue(response.getBody().contains("MANUAL"));
        assertTrue(response.getBody().contains("\"dezenas\":[5,10,15,20,25,30]"));
    }

    @Test
    void criarJogoAutomaticoRetorna201() {
        fonteAleatoriedadeFake.setProximasDezenas(List.of(3, 7, 14, 28, 42, 55));
        String body = """
                {"quantidade":6}
                """;
        ResponseEntity<String> response = post("/api/jogos", "ativo-token", body);
        assertEquals(201, response.getStatusCode().value());
        assertTrue(response.getBody().contains("AUTOMATICO"));
        assertTrue(response.getBody().contains("3"));
        assertTrue(response.getBody().contains("55"));
    }

    @Test
    void criarJogoComDezenasEQuantidadeRetorna400() {
        String body = """
                {"dezenas":[1,2,3,4,5,6],"quantidade":6}
                """;
        ResponseEntity<String> response = post("/api/jogos", "ativo-token", body);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void criarJogoComMenosDe6DezenasRetorna400() {
        String body = """
                {"dezenas":[1,2,3,4,5]}
                """;
        ResponseEntity<String> response = post("/api/jogos", "ativo-token", body);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void criarJogoComDezenasRepetidas() {
        String body = """
                {"dezenas":[1,1,3,4,5,6]}
                """;
        ResponseEntity<String> response = post("/api/jogos", "ativo-token", body);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void criarJogoComDezenaForaRange() {
        String body = """
                {"dezenas":[0,1,2,3,4,5]}
                """;
        ResponseEntity<String> response = post("/api/jogos", "ativo-token", body);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void listarJogosDoUsuario() {
        post("/api/jogos", "ativo-token", "{\"dezenas\":[1,2,3,4,5,6]}");
        post("/api/jogos", "ativo-token", "{\"dezenas\":[10,20,30,40,50,60]}");

        ResponseEntity<String> response = get("/api/jogos", "ativo-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("[1,2,3,4,5,6]"));
        assertTrue(response.getBody().contains("[10,20,30,40,50,60]"));
    }

    @Test
    void editarJogoAntesSorteio() {
        ResponseEntity<String> created = post("/api/jogos", "ativo-token", "{\"dezenas\":[1,2,3,4,5,6]}");
        String jogoId = extractId(created.getBody());

        ResponseEntity<String> updated = put("/api/jogos/" + jogoId, "ativo-token",
                "{\"dezenas\":[7,8,9,10,11,12]}");
        assertEquals(200, updated.getStatusCode().value());
        assertTrue(updated.getBody().contains("[7,8,9,10,11,12]"));
    }

    @Test
    void excluirJogoAntesSorteio() {
        ResponseEntity<String> created = post("/api/jogos", "ativo-token", "{\"dezenas\":[1,2,3,4,5,6]}");
        String jogoId = extractId(created.getBody());

        ResponseEntity<String> deleted = delete("/api/jogos/" + jogoId, "ativo-token");
        assertEquals(204, deleted.getStatusCode().value());
    }

    @Test
    void editarJogoAposSorteioRetorna409() {
        Concurso concurso = new Concurso();
        concurso.setNumero(9999);
        concurso.setDataSorteio(LocalDate.of(2024, 1, 1));
        concurso.setValorPremio(BigDecimal.valueOf(10000));
        concurso.addDezena(1);
        concurso.addDezena(2);
        concurso.addDezena(3);
        concurso.addDezena(4);
        concurso.addDezena(5);
        concurso.addDezena(6);
        concursoRepository.save(concurso);

        ResponseEntity<String> created = post("/api/jogos", "ativo-token",
                "{\"dezenas\":[1,2,3,4,5,6],\"concursoNumero\":9999}");
        String jogoId = extractId(created.getBody());

        ResponseEntity<String> updated = put("/api/jogos/" + jogoId, "ativo-token",
                "{\"dezenas\":[7,8,9,10,11,12]}");
        assertEquals(409, updated.getStatusCode().value());
    }

    @Test
    void contaPendenteBarradaDeJogos() {
        verificadorFake.registrarToken("pendente-token", "uid-pend-jogo", "pend-jogo@teste.com", true, MetodoLogin.SENHA);
        // Provision as PENDENTE
        get("/api/auth/me", "pendente-token");

        ResponseEntity<String> response = post("/api/jogos", "pendente-token", "{\"dezenas\":[1,2,3,4,5,6]}");
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void isolamentoEntreUsuarios() {
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        outro.setProviderUid("uid-outro");
        outro.setEmail("outro@teste.com");
        outro.setEstado(EstadoConta.ATIVO);
        outro.setPapel(Papel.USUARIO);
        outro.setMetodoLogin(MetodoLogin.SENHA);
        usuarioRepository.save(outro);
        verificadorFake.registrarToken("outro-token", "uid-outro", "outro@teste.com", true, MetodoLogin.SENHA);

        post("/api/jogos", "ativo-token", "{\"dezenas\":[1,2,3,4,5,6]}");

        ResponseEntity<String> response = get("/api/jogos", "outro-token");
        assertEquals(200, response.getStatusCode().value());
        assertEquals("[]", response.getBody());
    }

    private String extractId(String json) {
        int start = json.indexOf("\"id\":\"") + 6;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
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
        return restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> put(String path, String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url(path), HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> delete(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return restTemplate.exchange(url(path), HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
