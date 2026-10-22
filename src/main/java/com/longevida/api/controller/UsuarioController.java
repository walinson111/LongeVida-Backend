package com.longevida.api.controller;

import com.longevida.api.dto.UsuarioDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dados do próprio usuário autenticado. Não há rotas com id de usuário: ninguém acessa dados de terceiros. */
@RestController
@RequestMapping("/api/usuarios/me")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public UsuarioDto.Resposta obter(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return usuarioService.obter(usuario.getId());
    }

    @PutMapping
    public UsuarioDto.Resposta atualizar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                         @Valid @RequestBody UsuarioDto.AtualizarRequest req) {
        return usuarioService.atualizar(usuario.getId(), req);
    }

    @GetMapping("/configuracoes")
    public UsuarioDto.ConfiguracaoResposta obterConfiguracao(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return usuarioService.obterConfiguracao(usuario.getId());
    }

    @PutMapping("/configuracoes")
    public UsuarioDto.ConfiguracaoResposta atualizarConfiguracao(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                                 @Valid @RequestBody UsuarioDto.ConfiguracaoRequest req) {
        return usuarioService.atualizarConfiguracao(usuario.getId(), req);
    }
}
