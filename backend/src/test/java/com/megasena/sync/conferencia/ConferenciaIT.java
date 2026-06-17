package com.megasena.sync.conferencia;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.identidade.EstadoConta;
import com.megasena.sync.identidade.EventoIdentidadeRepository;
import com.megasena.sync.identidade.MetodoLogin;
import com.megasena.sync.identidade.Papel;
import com.megasena.sync.identidade.Usuario;
import com.megasena.sync.identidade.UsuarioRepository;
import com.megasena.sync.jogo.EventoJogoRepository;
import com.megasena.sync.jogo.JogoRepository;
import com.megasena.sync.moderacao.DecisaoModeracaoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConferenciaIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private VerificadorDeIdentidadeFake verificadorFake;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ConcursoRepository concursoRepository;
    @Autowired private JogoRepository jogoRepository;
    @Autowired private EventoJogoRepository eventoJogoRepository;
    @Autowired private EventoConferenciaRepository eventoConferenciaRepository;
    @Autowired private EventoIdentidadeRepository eventoIdentidadeRepository;
    @Autowired private DecisaoModeracaoRepository decisaoModeracaoRepository;

    @BeforeEach
    void setUp() {
        verificadorFake.limpar();
        eventoConferenciaRepository.deleteAll();
        eventoJogoRepository.deleteAll();
        jogoRepository.deleteAll();
        decisaoModeracaoRepository.deleteAll();
        eventoIdentidadeRepository.deleteAll();
        usuarioRepository.deleteAll();
        concursoRepository.deleteAll();

        Usuario ativo = new Usuario();
        ativo.setId(UUID.randomUUID());
        ativo.setProviderUid("uid-conf");
        ativo.setEmail("conf@teste.com");
        ativo.setEstado(EstadoConta.ATIVO);
        ativo.setPapel(Papel.USUARIO);
        ativo.setMetodoLogin(MetodoLogin.SENHA);
        usuarioRepository.save(ativo);
        verificadorFake.registrarToken("conf-token", "uid-conf", "conf@teste.com", true, MetodoLogin.SENHA);

        Concurso concurso = new Concurso();
        concurso.setNumero(1000);
        concurso.setDataSorteio(LocalDate.of(2024, 6, 1));
        concurso.setValorPremio(BigDecimal.valueOf(50000000));
        concurso.addDezena(5);
        concurso.addDezena(10);
        concurso.addDezena(15);
        concurso.addDezena(20);
        concurso.addDezena(25);
        concurso.addDezena(30);
        concursoRepository.save(concurso);

        Concurso aberto = new Concurso();
        aberto.setNumero(1001);
        aberto.setDataSorteio(LocalDate.of(2099, 12, 31));
        aberto.setValorPremio(BigDecimal.valueOf(10000000));
        concursoRepository.save(aberto);
    }

    @Test
    void conferirSenaRetornaSeis() {
        String jogoId = criarJogo("{\"dezenas\":[5,10,15,20,25,30],\"concursoNumero\":1000}");

        ResponseEntity<String> response = get("/api/jogos/" + jogoId + "/conferencia", "conf-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"acertos\":6"));
        assertTrue(response.getBody().contains("\"faixa\":\"SENA\""));
        assertTrue(response.getBody().contains("\"premiado\":true"));
    }

    @Test
    void conferirQuinaRetornaCinco() {
        String jogoId = criarJogo("{\"dezenas\":[5,10,15,20,25,60],\"concursoNumero\":1000}");

        ResponseEntity<String> response = get("/api/jogos/" + jogoId + "/conferencia", "conf-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"acertos\":5"));
        assertTrue(response.getBody().contains("\"faixa\":\"QUINA\""));
        assertTrue(response.getBody().contains("\"premiado\":true"));
    }

    @Test
    void conferirQuadraRetornaQuatro() {
        String jogoId = criarJogo("{\"dezenas\":[5,10,15,20,55,60],\"concursoNumero\":1000}");

        ResponseEntity<String> response = get("/api/jogos/" + jogoId + "/conferencia", "conf-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"acertos\":4"));
        assertTrue(response.getBody().contains("\"faixa\":\"QUADRA\""));
        assertTrue(response.getBody().contains("\"premiado\":true"));
    }

    @Test
    void conferirNenhumaComTresAcertos() {
        String jogoId = criarJogo("{\"dezenas\":[5,10,15,50,55,60],\"concursoNumero\":1000}");

        ResponseEntity<String> response = get("/api/jogos/" + jogoId + "/conferencia", "conf-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"acertos\":3"));
        assertTrue(response.getBody().contains("\"faixa\":\"NENHUMA\""));
        assertTrue(response.getBody().contains("\"premiado\":false"));
    }

    @Test
    void conferirConcursoAguardandoSorteio() {
        String jogoId = criarJogo("{\"dezenas\":[1,2,3,4,5,6],\"concursoNumero\":1001}");

        ResponseEntity<String> response = get("/api/jogos/" + jogoId + "/conferencia", "conf-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("AGUARDANDO_SORTEIO"));
    }

    @Test
    void conferenciaIdempotente() {
        String jogoId = criarJogo("{\"dezenas\":[5,10,15,20,25,30],\"concursoNumero\":1000}");

        get("/api/jogos/" + jogoId + "/conferencia", "conf-token");
        get("/api/jogos/" + jogoId + "/conferencia", "conf-token");

        long count = eventoConferenciaRepository.findAll().stream()
                .filter(e -> e.getJogoId().toString().equals(jogoId)).count();
        assertEquals(1, count);
    }

    @Test
    void listarConferenciasDoUsuario() {
        criarJogo("{\"dezenas\":[5,10,15,20,25,30],\"concursoNumero\":1000}");
        criarJogo("{\"dezenas\":[1,2,3,4,5,6],\"concursoNumero\":1001}");

        ResponseEntity<String> response = get("/api/conferencias", "conf-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("CONFERIDO") || response.getBody().contains("AGUARDANDO_SORTEIO"));
    }

    @Test
    void conferirComNoveDezenasCalculaCorretamente() {
        String jogoId = criarJogo("{\"dezenas\":[5,10,15,20,25,30,35,40,45],\"concursoNumero\":1000}");

        ResponseEntity<String> response = get("/api/jogos/" + jogoId + "/conferencia", "conf-token");
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("\"acertos\":6"));
        assertTrue(response.getBody().contains("\"faixa\":\"SENA\""));
    }

    @Test
    void isolamentoConferenciaEntreUsuarios() {
        Usuario outro = new Usuario();
        outro.setId(UUID.randomUUID());
        outro.setProviderUid("uid-outro-conf");
        outro.setEmail("outro-conf@teste.com");
        outro.setEstado(EstadoConta.ATIVO);
        outro.setPapel(Papel.USUARIO);
        outro.setMetodoLogin(MetodoLogin.SENHA);
        usuarioRepository.save(outro);
        verificadorFake.registrarToken("outro-conf-token", "uid-outro-conf", "outro-conf@teste.com", true, MetodoLogin.SENHA);

        String jogoId = criarJogo("{\"dezenas\":[5,10,15,20,25,30],\"concursoNumero\":1000}");

        ResponseEntity<String> response = get("/api/jogos/" + jogoId + "/conferencia", "outro-conf-token");
        assertEquals(404, response.getStatusCode().value());
    }

    private String criarJogo(String body) {
        ResponseEntity<String> response = post("/api/jogos", "conf-token", body);
        return extractId(response.getBody());
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

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
