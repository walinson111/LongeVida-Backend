package com.longevida.api.controller;

import com.longevida.api.dto.NotificacaoDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.LembreteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lembretes")
public class LembreteController {

    private final LembreteService lembreteService;

    public LembreteController(LembreteService lembreteService) {
        this.lembreteService = lembreteService;
    }

    @GetMapping
    public List<NotificacaoDto.LembreteResposta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return lembreteService.listar(usuario.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificacaoDto.LembreteResposta criar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                 @Valid @RequestBody NotificacaoDto.LembreteRequest req) {
        return lembreteService.criar(usuario.getId(), req);
    }

    @PutMapping("/{id}")
    public NotificacaoDto.LembreteResposta atualizar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody NotificacaoDto.LembreteRequest req) {
        return lembreteService.atualizar(usuario.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        lembreteService.excluir(usuario.getId(), id);
    }
}
