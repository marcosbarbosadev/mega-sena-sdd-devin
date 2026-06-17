package com.megasena.sync.moderacao;

import com.megasena.sync.identidade.EstadoConta;
import com.megasena.sync.identidade.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ModeracaoService {

    private final UsuarioRepository usuarioRepository;
    private final DecisaoModeracaoRepository decisaoRepository;

    public ModeracaoService(UsuarioRepository usuarioRepository,
                            DecisaoModeracaoRepository decisaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.decisaoRepository = decisaoRepository;
    }

    public List<ContaPendenteResponse> listarPendentes() {
        return usuarioRepository.findByEstado(EstadoConta.PENDENTE).stream()
                .map(ContaPendenteResponse::from)
                .toList();
    }

    @Transactional
    public void aprovar(UUID usuarioId, UUID adminId) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ContaNaoEncontradaException("Conta não encontrada"));

        int updated = usuarioRepository.transicionarDePendente(usuarioId, EstadoConta.ATIVO, Instant.now());
        if (updated == 0) {
            throw new TransicaoInvalidaException("A conta não está em estado PENDENTE");
        }

        DecisaoModeracao decisao = new DecisaoModeracao();
        decisao.setUsuarioId(usuarioId);
        decisao.setAdminId(adminId);
        decisao.setDecisao(Decisao.APROVADO);
        decisaoRepository.save(decisao);
    }

    @Transactional
    public void reprovar(UUID usuarioId, UUID adminId, String motivo) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ContaNaoEncontradaException("Conta não encontrada"));

        int updated = usuarioRepository.transicionarDePendente(usuarioId, EstadoConta.REPROVADO, Instant.now());
        if (updated == 0) {
            throw new TransicaoInvalidaException("A conta não está em estado PENDENTE");
        }

        DecisaoModeracao decisao = new DecisaoModeracao();
        decisao.setUsuarioId(usuarioId);
        decisao.setAdminId(adminId);
        decisao.setDecisao(Decisao.REPROVADO);
        decisao.setMotivo(motivo);
        decisaoRepository.save(decisao);
    }
}
