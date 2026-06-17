package com.megasena.sync.jogo;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.conferencia.EventoConferenciaRepository;
import com.megasena.sync.config.GlobalExceptionHandler.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class JogoService {

    private final JogoRepository jogoRepository;
    private final EventoJogoRepository eventoJogoRepository;
    private final ConcursoRepository concursoRepository;
    private final FonteAleatoriedade fonteAleatoriedade;
    private final EventoConferenciaRepository eventoConferenciaRepository;

    public JogoService(JogoRepository jogoRepository,
                       EventoJogoRepository eventoJogoRepository,
                       ConcursoRepository concursoRepository,
                       FonteAleatoriedade fonteAleatoriedade,
                       EventoConferenciaRepository eventoConferenciaRepository) {
        this.jogoRepository = jogoRepository;
        this.eventoJogoRepository = eventoJogoRepository;
        this.concursoRepository = concursoRepository;
        this.fonteAleatoriedade = fonteAleatoriedade;
        this.eventoConferenciaRepository = eventoConferenciaRepository;
    }

    @Transactional
    public JogoResponse criar(JogoRequest request, UUID usuarioId) {
        List<Integer> dezenas;
        TipoSelecao tipoSelecao;

        boolean temDezenas = request.dezenas() != null && !request.dezenas().isEmpty();
        boolean temQuantidade = request.quantidade() != null;

        if (temDezenas && temQuantidade) {
            throw new JogoValidacaoException("Informe dezenas OU quantidade, não ambos.");
        }
        if (!temDezenas && !temQuantidade) {
            throw new JogoValidacaoException("Informe dezenas ou quantidade.");
        }

        if (temDezenas) {
            dezenas = request.dezenas();
            tipoSelecao = TipoSelecao.MANUAL;
            validarDezenas(dezenas);
        } else {
            validarQuantidade(request.quantidade());
            dezenas = fonteAleatoriedade.gerarDezenas(request.quantidade());
            tipoSelecao = TipoSelecao.AUTOMATICO;
        }

        Jogo jogo = new Jogo();
        jogo.setId(UUID.randomUUID());
        jogo.setUsuarioId(usuarioId);
        jogo.setTipoSelecao(tipoSelecao);
        jogo.setConcursoNumero(request.concursoNumero());
        dezenas.forEach(jogo::addDezena);

        jogo = jogoRepository.save(jogo);
        registrarEvento(jogo.getId(), usuarioId, TipoEventoJogo.CADASTRO);

        return JogoResponse.from(jogo);
    }

    public List<JogoResponse> listarPorUsuario(UUID usuarioId) {
        return jogoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(JogoResponse::from)
                .toList();
    }

    @Transactional
    public JogoResponse editar(UUID jogoId, JogoRequest request, UUID usuarioId) {
        Jogo jogo = jogoRepository.findByIdAndUsuarioId(jogoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));

        verificarEditavel(jogo);

        List<Integer> dezenas;
        boolean temDezenas = request.dezenas() != null && !request.dezenas().isEmpty();
        boolean temQuantidade = request.quantidade() != null;

        if (temDezenas && temQuantidade) {
            throw new JogoValidacaoException("Informe dezenas OU quantidade, não ambos.");
        }
        if (!temDezenas && !temQuantidade) {
            throw new JogoValidacaoException("Informe dezenas ou quantidade.");
        }

        if (temDezenas) {
            dezenas = request.dezenas();
            jogo.setTipoSelecao(TipoSelecao.MANUAL);
            validarDezenas(dezenas);
        } else {
            validarQuantidade(request.quantidade());
            dezenas = fonteAleatoriedade.gerarDezenas(request.quantidade());
            jogo.setTipoSelecao(TipoSelecao.AUTOMATICO);
        }

        if (request.concursoNumero() != null) {
            jogo.setConcursoNumero(request.concursoNumero());
        }

        jogo.getDezenas().clear();
        dezenas.forEach(jogo::addDezena);

        jogo = jogoRepository.save(jogo);
        registrarEvento(jogo.getId(), usuarioId, TipoEventoJogo.EDICAO);

        return JogoResponse.from(jogo);
    }

    @Transactional
    public void excluir(UUID jogoId, UUID usuarioId) {
        Jogo jogo = jogoRepository.findByIdAndUsuarioId(jogoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));

        verificarEditavel(jogo);
        eventoConferenciaRepository.deleteByJogoId(jogo.getId());
        eventoJogoRepository.deleteByJogoId(jogo.getId());
        jogoRepository.delete(jogo);
    }

    private void validarDezenas(List<Integer> dezenas) {
        if (dezenas.size() < 6 || dezenas.size() > 9) {
            throw new JogoValidacaoException("Quantidade de dezenas deve ser entre 6 e 9.");
        }
        if (new HashSet<>(dezenas).size() != dezenas.size()) {
            throw new JogoValidacaoException("Dezenas não podem se repetir.");
        }
        for (int d : dezenas) {
            if (d < 1 || d > 60) {
                throw new JogoValidacaoException("Dezenas devem estar entre 1 e 60.");
            }
        }
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade < 6 || quantidade > 9) {
            throw new JogoValidacaoException("Quantidade de dezenas deve ser entre 6 e 9.");
        }
    }

    private void verificarEditavel(Jogo jogo) {
        if (jogo.getConcursoNumero() != null) {
            Concurso concurso = concursoRepository.findByNumero(jogo.getConcursoNumero()).orElse(null);
            if (concurso != null && !concurso.getDezenas().isEmpty()) {
                throw new JogoNaoEditavelException("Jogo não pode ser editado após o sorteio do concurso.");
            }
        }
    }

    private void registrarEvento(UUID jogoId, UUID usuarioId, TipoEventoJogo tipo) {
        EventoJogo evento = new EventoJogo();
        evento.setJogoId(jogoId);
        evento.setUsuarioId(usuarioId);
        evento.setTipo(tipo);
        eventoJogoRepository.save(evento);
    }
}
