package com.megasena.sync.identidade;

import com.megasena.sync.config.IdentidadeProperties;
import com.megasena.sync.identidade.provedor.IdentidadeVerificada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProvisionamentoService {

    private static final Logger log = LoggerFactory.getLogger(ProvisionamentoService.class);

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaIdentidadeService auditoria;
    private final IdentidadeProperties properties;

    public ProvisionamentoService(UsuarioRepository usuarioRepository,
                                  AuditoriaIdentidadeService auditoria,
                                  IdentidadeProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
        this.properties = properties;
    }

    @Transactional
    public Usuario resolverOuCriar(IdentidadeVerificada identidade) {
        if (identidade.metodoLogin() == MetodoLogin.GOOGLE && !identidade.emailVerificado()) {
            throw new EmailNaoVerificadoException("Verifique seu e-mail no provedor antes de continuar.");
        }

        return usuarioRepository.findByProviderUid(identidade.uid())
                .orElseGet(() -> criarConta(identidade));
    }

    private Usuario criarConta(IdentidadeVerificada identidade) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setProviderUid(identidade.uid());
        usuario.setEmail(identidade.email());
        usuario.setMetodoLogin(identidade.metodoLogin());

        boolean isAdminBootstrap = properties.getAdminsBootstrap() != null
                && properties.getAdminsBootstrap().stream()
                    .anyMatch(e -> e.equalsIgnoreCase(identidade.email()));

        if (isAdminBootstrap) {
            usuario.setPapel(Papel.ADMINISTRADOR);
            usuario.setEstado(EstadoConta.ATIVO);
        } else {
            usuario.setPapel(Papel.USUARIO);
            usuario.setEstado(EstadoConta.PENDENTE);
        }

        try {
            usuario = usuarioRepository.save(usuario);
            log.info("Conta provisionada: uid={}, estado={}, papel={}", identidade.uid(), usuario.getEstado(), usuario.getPapel());
            auditoria.registrar(usuario.getId(), TipoEvento.CADASTRO, identidade.metodoLogin(), true, null);
            return usuario;
        } catch (DataIntegrityViolationException e) {
            return usuarioRepository.findByProviderUid(identidade.uid())
                    .orElseThrow(() -> e);
        }
    }
}
