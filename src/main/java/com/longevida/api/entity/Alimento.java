package com.longevida.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Item do catálogo de alimentos. Os valores nutricionais são por porção de referência. */
@Entity
@Table(name = "alimentos")
@Getter
@Setter
@NoArgsConstructor
public class Alimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "porcao_quantidade", nullable = false, precision = 8, scale = 2)
    private BigDecimal porcaoQuantidade;

    /** "g", "ml" ou "unidade". */
    @Column(name = "porcao_unidade", nullable = false, length = 10)
    private String porcaoUnidade;

    @Column(name = "calorias", nullable = false, precision = 8, scale = 1)
    private BigDecimal calorias;

    @Column(name = "proteinas", nullable = false, precision = 8, scale = 1)
    private BigDecimal proteinas;

    @Column(name = "carboidratos", nullable = false, precision = 8, scale = 1)
    private BigDecimal carboidratos;

    @Column(name = "gorduras", nullable = false, precision = 8, scale = 1)
    private BigDecimal gorduras;
}
