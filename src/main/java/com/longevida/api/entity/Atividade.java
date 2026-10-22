package com.longevida.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Atividade física registrada (execução de um treino). */
@Entity
@Table(name = "atividades")
@Getter
@Setter
@NoArgsConstructor
public class Atividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treino_id")
    private Treino treino;

    /** Nome do treino no momento do registro (permanece se o treino for apagado). */
    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "data_atividade", nullable = false)
    private LocalDate dataAtividade;

    @Column(name = "horario", nullable = false)
    private LocalTime horario;

    @Column(name = "duracao_min", nullable = false)
    private Integer duracaoMin;

    @Column(name = "calorias_gastas", nullable = false)
    private Integer caloriasGastas;

    @Column(name = "exercicios_concluidos", nullable = false)
    private Integer exerciciosConcluidos;

    @Column(name = "exercicios_total", nullable = false)
    private Integer exerciciosTotal;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void aoInserir() {
        this.criadoEm = Instant.now();
    }
}
