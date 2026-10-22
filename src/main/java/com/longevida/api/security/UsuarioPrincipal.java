package com.longevida.api.security;

import com.longevida.api.entity.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Usuário autenticado exposto ao Spring Security e aos controllers (@AuthenticationPrincipal). */
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String senhaHash;
    private final boolean ativo;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
        this.ativo = usuario.isAtivo();
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USUARIO"));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
