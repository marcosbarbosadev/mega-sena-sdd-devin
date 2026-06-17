package com.megasena.sync.config;

import com.megasena.sync.identidade.EstadoConta;
import com.megasena.sync.identidade.Papel;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UsuarioAutenticado implements UserDetails {

    private final UUID usuarioId;
    private final String email;
    private final Papel papel;
    private final EstadoConta estado;
    private final String providerUid;

    public UsuarioAutenticado(UUID usuarioId, String email, Papel papel, EstadoConta estado, String providerUid) {
        this.usuarioId = usuarioId;
        this.email = email;
        this.papel = papel;
        this.estado = estado;
        this.providerUid = providerUid;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (estado == EstadoConta.ATIVO) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USUARIO"));
            if (papel == Papel.ADMINISTRADOR) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"));
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return email; }

    public UUID getUsuarioId() { return usuarioId; }
    public String getEmail() { return email; }
    public Papel getPapel() { return papel; }
    public EstadoConta getEstado() { return estado; }
    public String getProviderUid() { return providerUid; }
}
