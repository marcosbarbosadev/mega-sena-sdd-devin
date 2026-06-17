package com.megasena.sync.conferencia;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoDezena;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.config.GlobalExceptionHandler.ResourceNotFoundException;
import com.megasena.sync.jogo.Jogo;
import com.megasena.sync.jogo.JogoDezena;
import com.megasena.sync.jogo.JogoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ConferenciaService {

    private final JogoRepository jogoRepository;
    private final ConcursoRepository concursoRepository;
    private final EventoConferenciaRepository eventoConferenciaRepository;

    public ConferenciaService(JogoRepository jogoRepository,
                              ConcursoRepository concursoRepository,
                              EventoConferenciaRepository eventoConferenciaRepository) {
        this.jogoRepository = jogoRepository;
        this.concursoRepository = concursoRepository;
        this.eventoConferenciaRepository = eventoConferenciaRepository;
    }

    @Transactional
    public ConferenciaResponse conferir(UUID jogoId, UUID usuarioId) {
        Jogo jogo = jogoRepository.findByIdAndUsuarioId(jogoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));

        if (jogo.getConcursoNumero() == null) {
            throw new ResourceNotFoundException("Jogo sem concurso associado");
        }

        Concurso concurso = concursoRepository.findByNumero(jogo.getConcursoNumero())
                .orElseThrow(() -> new ResourceNotFoundException("Concurso não encontrado"));

        List<Integer> dezenasJogadas = jogo.getDezenas().stream()
                .map(JogoDezena::getDezena).sorted().toList();
        List<Integer> dezenasSorteadas = concurso.getDezenas().stream()
                .map(ConcursoDezena::getDezena).sorted().toList();

        if (dezenasSorteadas.isEmpty()) {
            return new ConferenciaResponse(
                    jogo.getId(), concurso.getNumero(), dezenasJogadas,
                    List.of(), 0, Faixa.NENHUMA, false, "AGUARDANDO_SORTEIO"
            );
        }

        Set<Integer> setaSorteio = new HashSet<>(dezenasSorteadas);
        int acertos = (int) dezenasJogadas.stream().filter(setaSorteio::contains).count();
        Faixa faixa = calcularFaixa(acertos);
        boolean premiado = acertos >= 4;

        registrarEventoIdempotente(jogo.getId(), concurso.getNumero(), usuarioId, acertos, faixa, premiado);

        return new ConferenciaResponse(
                jogo.getId(), concurso.getNumero(), dezenasJogadas,
                dezenasSorteadas, acertos, faixa, premiado, "CONFERIDO"
        );
    }

    public List<ConferenciaResponse> listarConferencias(UUID usuarioId) {
        List<Jogo> jogos = jogoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId);

        return jogos.stream()
                .filter(j -> j.getConcursoNumero() != null)
                .map(jogo -> conferirSemRegistro(jogo))
                .toList();
    }

    private ConferenciaResponse conferirSemRegistro(Jogo jogo) {
        Concurso concurso = concursoRepository.findByNumero(jogo.getConcursoNumero()).orElse(null);

        List<Integer> dezenasJogadas = jogo.getDezenas().stream()
                .map(JogoDezena::getDezena).sorted().toList();

        if (concurso == null || concurso.getDezenas().isEmpty()) {
            return new ConferenciaResponse(
                    jogo.getId(), jogo.getConcursoNumero(), dezenasJogadas,
                    List.of(), 0, Faixa.NENHUMA, false, "AGUARDANDO_SORTEIO"
            );
        }

        List<Integer> dezenasSorteadas = concurso.getDezenas().stream()
                .map(ConcursoDezena::getDezena).sorted().toList();
        Set<Integer> setaSorteio = new HashSet<>(dezenasSorteadas);
        int acertos = (int) dezenasJogadas.stream().filter(setaSorteio::contains).count();
        Faixa faixa = calcularFaixa(acertos);
        boolean premiado = acertos >= 4;

        return new ConferenciaResponse(
                jogo.getId(), jogo.getConcursoNumero(), dezenasJogadas,
                dezenasSorteadas, acertos, faixa, premiado, "CONFERIDO"
        );
    }

    private Faixa calcularFaixa(int acertos) {
        return switch (acertos) {
            case 6 -> Faixa.SENA;
            case 5 -> Faixa.QUINA;
            case 4 -> Faixa.QUADRA;
            default -> Faixa.NENHUMA;
        };
    }

    private void registrarEventoIdempotente(UUID jogoId, Integer concursoNumero,
                                            UUID usuarioId, int acertos, Faixa faixa, boolean premiado) {
        if (eventoConferenciaRepository.findByJogoIdAndConcursoNumero(jogoId, concursoNumero).isPresent()) {
            return;
        }
        EventoConferencia evento = new EventoConferencia();
        evento.setJogoId(jogoId);
        evento.setConcursoNumero(concursoNumero);
        evento.setUsuarioId(usuarioId);
        evento.setAcertos(acertos);
        evento.setFaixa(faixa);
        evento.setPremiado(premiado);
        try {
            eventoConferenciaRepository.save(evento);
        } catch (DataIntegrityViolationException e) {
            // Idempotent - already registered
        }
    }
}
