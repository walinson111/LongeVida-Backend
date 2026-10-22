package com.longevida.api.dto;

import java.math.BigDecimal;

public final class AlimentoDto {

    private AlimentoDto() {}

    /** "porcao" é o texto de exibição (ex.: "100g", "1 unidade"). */
    public record Resposta(
            Long id,
            String nome,
            String porcao,
            BigDecimal porcaoQuantidade,
            String porcaoUnidade,
            BigDecimal calorias,
            BigDecimal proteinas,
            BigDecimal carboidratos,
            BigDecimal gorduras) {}
}
