package com.longevida.api.controller;

import com.longevida.api.dto.ComunidadeDto;
import com.longevida.api.dto.DietaDto;
import com.longevida.api.dto.TreinoDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.DietaService;
import com.longevida.api.service.ProfissionalService;
import com.longevida.api.service.TreinoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Conteúdo público: profissionais, seguidores e importação de dietas/treinos publicados. */
@RestController
@RequestMapping("/api/comunidade")
public class ComunidadeController {

    private final ProfissionalService profissionalService;
    private final DietaService dietaService;
    private final TreinoService treinoService;

    public ComunidadeController(ProfissionalService profissionalService, DietaService dietaService,
                                TreinoService treinoService) {
        this.profissionalService = profissionalService;
        this.dietaService = dietaService;
        this.treinoService = treinoService;
    }

    @GetMapping("/profissionais")
    public List<ComunidadeDto.ProfissionalResposta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return profissionalService.listar(usuario.getId());
    }

    @GetMapping("/profissionais/{id}")
    public ComunidadeDto.ProfissionalResposta detalhar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                       @PathVariable Long id) {
        return profissionalService.detalhar(usuario.getId(), id);
    }

    @PostMapping("/profissionais/{id}/seguir")
    public ComunidadeDto.ProfissionalResposta seguir(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                     @PathVariable Long id) {
        return profissionalService.seguir(usuario.getId(), id);
    }

    @DeleteMapping("/profissionais/{id}/seguir")
    public ComunidadeDto.ProfissionalResposta deixarDeSeguir(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                             @PathVariable Long id) {
        return profissionalService.deixarDeSeguir(usuario.getId(), id);
    }

    @GetMapping("/dietas/{id}")
    public DietaDto.PublicaResposta dietaPublica(@PathVariable Long id) {
        return dietaService.obterPublica(id);
    }

    @PostMapping("/dietas/{id}/importar")
    @ResponseStatus(HttpStatus.CREATED)
    public DietaDto.Resposta importarDieta(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return dietaService.importar(usuario.getId(), id);
    }

    @GetMapping("/treinos/{id}")
    public TreinoDto.PublicaResposta treinoPublico(@PathVariable Long id) {
        return treinoService.obterPublico(id);
    }

    @PostMapping("/treinos/{id}/importar")
    @ResponseStatus(HttpStatus.CREATED)
    public TreinoDto.Resposta importarTreino(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return treinoService.importar(usuario.getId(), id);
    }
}
