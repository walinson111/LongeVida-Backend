package com.longevida.api.controller;

import com.longevida.api.dto.AlimentoDto;
import com.longevida.api.service.AlimentoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alimentos")
public class AlimentoController {

    private final AlimentoService alimentoService;

    public AlimentoController(AlimentoService alimentoService) {
        this.alimentoService = alimentoService;
    }

    @GetMapping
    public List<AlimentoDto.Resposta> listar(@RequestParam(required = false) String busca) {
        return alimentoService.listar(busca);
    }
}
