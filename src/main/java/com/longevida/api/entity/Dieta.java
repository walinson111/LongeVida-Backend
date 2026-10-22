package com.longevida.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dietas")
@Getter
@Setter
@NoArgsConstructor
public class Dieta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "calorias", nullable = false)
    private Integer calorias;

    @Column(name = "proteinas", nullable = false, precision = 6, scale = 1)
    private BigDecimal proteinas = BigDecimal.ZERO;

    @Column(name = "carboidratos", nullable = false, precision = 6, scale = 1)
    private BigDecimal carboidratos = BigDecimal.ZERO;

    @Column(name = "gorduras", nullable = false, precision = 6, scale = 1)
    private BigDecimal gorduras = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Visibilidade status = Visibilidade.PRIVADA;

    /** Preenchido quando a dieta é uma cópia importada de uma dieta publicada. */
    @Column(name = "origem_dieta_id")
    private Long origemDietaId;

    /** Nome de quem publicou o original (crédito mantido mesmo se o original for apagado). */
    @Column(name = "credito_nome", length = 120)
    private String creditoNome;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void aoInserir() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }
}
