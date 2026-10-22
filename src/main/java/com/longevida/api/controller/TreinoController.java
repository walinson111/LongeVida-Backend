package com.longevida.api.controller;

import com.longevida.api.dto.TreinoDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.TreinoService;
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
@RequestMapping("/api/treinos")
public class TreinoController {

    private final TreinoService treinoService;

    public TreinoController(TreinoService treinoService) {
        this.treinoService = treinoService;
    }

    @GetMapping
    public List<TreinoDto.Resposta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return treinoService.listar(usuario.getId());
    }

    @GetMapping("/{id}")
    public TreinoDto.Resposta obter(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return treinoService.obter(usuario.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TreinoDto.Resposta criar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                   @Valid @RequestBody TreinoDto.Request req) {
        return treinoService.criar(usuario.getId(), req);
    }

    @PutMapping("/{id}")
    public TreinoDto.Resposta atualizar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
                                       @Valid @RequestBody TreinoDto.Request req) {
        return treinoService.atualizar(usuario.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        treinoService.excluir(usuario.getId(), id);
    }

    @PostMapping("/{id}/publicar")
    public TreinoDto.Resposta publicar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return treinoService.publicar(usuario.getId(), id, true);
    }

    @PostMapping("/{id}/despublicar")
    public TreinoDto.Resposta despublicar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return treinoService.publicar(usuario.getId(), id, false);
    }
}
