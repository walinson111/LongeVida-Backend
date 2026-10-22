package com.longevida.api.service;

import com.longevida.api.dto.AlimentoDto;
import com.longevida.api.entity.Alimento;
import com.longevida.api.repository.AlimentoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlimentoService {

    private final AlimentoRepository alimentoRepository;

    public AlimentoService(AlimentoRepository alimentoRepository) {
        this.alimentoRepository = alimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<AlimentoDto.Resposta> listar(String busca) {
        List<Alimento> alimentos = busca == null || busca.isBlank()
                ? alimentoRepository.findAllByOrderByNomeAsc()
                : alimentoRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(busca.trim());
        return alimentos.stream().map(AlimentoService::paraDto).toList();
    }

    private static AlimentoDto.Resposta paraDto(Alimento a) {
        String quantidade = a.getPorcaoQuantidade().stripTrailingZeros().toPlainString();
        String porcao = "unidade".equals(a.getPorcaoUnidade())
                ? quantidade + " unidade"
                : quantidade + a.getPorcaoUnidade();
        return new AlimentoDto.Resposta(a.getId(), a.getNome(), porcao, a.getPorcaoQuantidade(), a.getPorcaoUnidade(),
                a.getCalorias(), a.getProteinas(), a.getCarboidratos(), a.getGorduras());
    }
}
