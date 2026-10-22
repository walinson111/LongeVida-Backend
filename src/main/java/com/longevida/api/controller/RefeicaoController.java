package com.longevida.api.controller;

import com.longevida.api.dto.RefeicaoDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.RefeicaoService;
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
@RequestMapping("/api/refeicoes")
public class RefeicaoController {

    private final RefeicaoService refeicaoService;

    public RefeicaoController(RefeicaoService refeicaoService) {
        this.refeicaoService = refeicaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefeicaoDto.Resposta registrar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                          @Valid @RequestBody RefeicaoDto.Request req) {
        return refeicaoService.registrar(usuario.getId(), req);
    }

    @GetMapping
    public List<RefeicaoDto.Resposta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return refeicaoService.listar(usuario.getId(), de, ate);
    }

    @GetMapping("/{id}")
    public RefeicaoDto.Resposta obter(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return refeicaoService.obter(usuario.getId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        refeicaoService.excluir(usuario.getId(), id);
    }
}
