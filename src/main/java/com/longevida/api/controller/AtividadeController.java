package com.longevida.api.controller;

import com.longevida.api.dto.AtividadeDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.AtividadeService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atividades")
public class AtividadeController {

    private final AtividadeService atividadeService;

    public AtividadeController(AtividadeService atividadeService) {
        this.atividadeService = atividadeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeDto.Resposta registrar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                           @Valid @RequestBody AtividadeDto.Request req) {
        return atividadeService.registrar(usuario.getId(), req);
    }

    @GetMapping
    public List<AtividadeDto.Resposta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return atividadeService.listar(usuario.getId(), de, ate);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        atividadeService.excluir(usuario.getId(), id);
    }
}
