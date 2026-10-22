package com.longevida.api.controller;

import com.longevida.api.dto.ComunidadeDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.ProfissionalService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil profissional do próprio usuário. */
@RestController
@RequestMapping("/api/profissionais/me")
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    public ProfissionalController(ProfissionalService profissionalService) {
        this.profissionalService = profissionalService;
    }

    @GetMapping
    public ComunidadeDto.MeuPerfilResposta obter(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return profissionalService.meuPerfil(usuario.getId());
    }

    @PutMapping
    public ComunidadeDto.MeuPerfilResposta atualizar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                     @Valid @RequestBody ComunidadeDto.MeuPerfilRequest req) {
        return profissionalService.atualizarMeuPerfil(usuario.getId(), req);
    }
}
