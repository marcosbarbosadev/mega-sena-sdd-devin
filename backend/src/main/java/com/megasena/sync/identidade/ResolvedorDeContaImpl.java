package com.megasena.sync.identidade;

import com.megasena.sync.config.UsuarioAutenticado;
import com.megasena.sync.identidade.provedor.IdentidadeVerificada;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ResolvedorDeContaImpl implements ResolvedorDeConta {

    private final ProvisionamentoService provisionamentoService;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaIdentidadeService auditoria;

    public ResolvedorDeContaImpl(ProvisionamentoService provisionamentoService,
                                 UsuarioRepository usuarioRepository,
                                 AuditoriaIdentidadeService auditoria) {
        this.provisionamentoService = provisionamentoService;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public UsuarioAutenticado resolver(IdentidadeVerificada identidade) {
        Usuario usuario = provisionamentoService.resolverOuCriar(identidade);
        usuario.setUltimoAcessoEm(Instant.now());
        usuarioRepository.save(usuario);
        auditoria.registrar(usuario.getId(), TipoEvento.AUTENTICACAO, identidade.metodoLogin(), true, null);
        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.getEstado(),
                usuario.getProviderUid()
        );
    }
}
