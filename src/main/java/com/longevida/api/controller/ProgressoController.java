package com.longevida.api.controller;

import com.longevida.api.dto.ProgressoDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.ProgressoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Resumo diário, histórico, progresso (gráficos), peso e água. */
@RestController
@RequestMapping("/api")
public class ProgressoController {

    private final ProgressoService progressoService;

    public ProgressoController(ProgressoService progressoService) {
        this.progressoService = progressoService;
    }

    @GetMapping("/resumo-diario")
    public ProgressoDto.ResumoDiario resumoDiario(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return progressoService.resumoDiario(usuario.getId(), data);
    }

    @GetMapping("/historico")
    public List<ProgressoDto.ItemHistorico> historico(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                      @RequestParam(required = false) ProgressoDto.TipoHistorico tipo,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return progressoService.historico(usuario.getId(), tipo, de, ate);
    }

    @GetMapping("/progresso")
    public ProgressoDto.Progresso progresso(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                            @RequestParam(defaultValue = "7") int dias) {
        return progressoService.progresso(usuario.getId(), dias);
    }

    @PostMapping("/progresso/peso")
    public ProgressoDto.PontoPeso registrarPeso(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                @Valid @RequestBody ProgressoDto.PesoRequest req) {
        return progressoService.registrarPeso(usuario.getId(), req.peso(), req.data());
    }

    @PostMapping("/agua")
    public ProgressoDto.AguaResposta registrarAgua(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                   @Valid @RequestBody ProgressoDto.AguaRequest req) {
        return progressoService.registrarAgua(usuario.getId(), req.quantidadeMl());
    }
}
