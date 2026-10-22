package com.longevida.api.controller;

import com.longevida.api.dto.NotificacaoDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.NotificacaoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public List<NotificacaoDto.Resposta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return notificacaoService.listar(usuario.getId());
    }

    @GetMapping("/nao-lidas")
    public NotificacaoDto.Contagem naoLidas(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return notificacaoService.contarNaoLidas(usuario.getId());
    }

    @PatchMapping("/{id}/lida")
    public NotificacaoDto.Resposta marcarComoLida(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                  @PathVariable Long id) {
        return notificacaoService.marcarComoLida(usuario.getId(), id);
    }

    @PostMapping("/marcar-todas-lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void marcarTodasComoLidas(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        notificacaoService.marcarTodasComoLidas(usuario.getId());
    }
}
