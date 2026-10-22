package com.longevida.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Alimento consumido em uma refeição, com os valores nutricionais já calculados. */
@Entity
@Table(name = "refeicao_itens")
@Getter
@Setter
@NoArgsConstructor
public class RefeicaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refeicao_id", nullable = false)
    private Refeicao refeicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_id")
    private Alimento alimento;

    @Column(name = "nome_alimento", nullable = false, length = 120)
    private String nomeAlimento;

    @Column(name = "quantidade", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "unidade", nullable = false, length = 10)
    private String unidade;

    @Column(name = "calorias", nullable = false)
    private Integer calorias;

    @Column(name = "proteinas", nullable = false, precision = 8, scale = 1)
    private BigDecimal proteinas;

    @Column(name = "carboidratos", nullable = false, precision = 8, scale = 1)
    private BigDecimal carboidratos;

    @Column(name = "gorduras", nullable = false, precision = 8, scale = 1)
    private BigDecimal gorduras;
}
