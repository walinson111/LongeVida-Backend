package com.longevida.api.entity;

import java.math.BigDecimal;

/** Tipos de dados aceitos dos wearables, com unidade e limite máximo plausível. */
public enum TipoMedicao {
    PASSOS("passos", new BigDecimal("200000")),
    FREQUENCIA_CARDIACA("bpm", new BigDecimal("250")),
    CALORIAS_GASTAS("kcal", new BigDecimal("20000")),
    SONO_MINUTOS("min", new BigDecimal("1440"));

    private final String unidade;
    private final BigDecimal maximo;

    TipoMedicao(String unidade, BigDecimal maximo) {
        this.unidade = unidade;
        this.maximo = maximo;
    }

    public String getUnidade() {
        return unidade;
    }

    public BigDecimal getMaximo() {
        return maximo;
    }
}
