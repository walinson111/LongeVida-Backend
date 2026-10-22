package com.longevida.api.controller;

import com.longevida.api.dto.DispositivoDto;
import com.longevida.api.security.UsuarioPrincipal;
import com.longevida.api.service.DispositivoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispositivos")
public class DispositivoController {

    private final DispositivoService dispositivoService;

    public DispositivoController(DispositivoService dispositivoService) {
        this.dispositivoService = dispositivoService;
    }

    @GetMapping
    public List<DispositivoDto.Resposta> listar(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return dispositivoService.listar(usuario.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DispositivoDto.Resposta cadastrar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                             @Valid @RequestBody DispositivoDto.Request req) {
        return dispositivoService.cadastrar(usuario.getId(), req);
    }

    @PatchMapping("/{id}/conexao")
    public DispositivoDto.Resposta alterarConexao(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody DispositivoDto.ConexaoRequest req) {
        return dispositivoService.alterarConexao(usuario.getId(), id, req.conectado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        dispositivoService.remover(usuario.getId(), id);
    }

    @PostMapping("/{id}/medicoes")
    public DispositivoDto.IngestaoResposta registrarMedicoes(@AuthenticationPrincipal UsuarioPrincipal usuario,
                                                             @PathVariable Long id,
                                                             @Valid @RequestBody DispositivoDto.MedicoesRequest req) {
        return dispositivoService.registrarMedicoes(usuario.getId(), id, req);
    }

    @GetMapping("/medicoes/ultimas")
    public List<DispositivoDto.MedicaoResposta> ultimasMedicoes(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return dispositivoService.ultimasMedicoes(usuario.getId());
    }
}
