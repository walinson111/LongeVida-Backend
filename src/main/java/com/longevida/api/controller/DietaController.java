package com.longevida.api.controller;

import com.longevida.api.dto.DietaDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.DietaService;
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
@RequestMapping("/api/dietas")
public class DietaController {

    private final DietaService dietaService;

    public DietaController(DietaService dietaService) {
        this.dietaService = dietaService;
    }

    @GetMapping
    public List<DietaDto.Resposta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return dietaService.listar(usuario.getId());
    }

    @GetMapping("/{id}")
    public DietaDto.Resposta obter(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return dietaService.obter(usuario.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DietaDto.Resposta criar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                   @Valid @RequestBody DietaDto.Request req) {
        return dietaService.criar(usuario.getId(), req);
    }

    @PutMapping("/{id}")
    public DietaDto.Resposta atualizar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
                                       @Valid @RequestBody DietaDto.Request req) {
        return dietaService.atualizar(usuario.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        dietaService.excluir(usuario.getId(), id);
    }

    @PostMapping("/{id}/publicar")
    public DietaDto.Resposta publicar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return dietaService.publicar(usuario.getId(), id, true);
    }

    @PostMapping("/{id}/despublicar")
    public DietaDto.Resposta despublicar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return dietaService.publicar(usuario.getId(), id, false);
    }
}
