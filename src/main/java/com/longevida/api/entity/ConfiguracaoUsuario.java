package com.longevida.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Preferências e metas diárias do usuário (1 linha por usuário). */
@Entity
@Table(name = "configuracoes_usuario")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracaoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "notificacoes_ativas", nullable = false)
    private boolean notificacoesAtivas = true;

    @Column(name = "meta_agua_ml", nullable = false)
    private int metaAguaMl = 2000;

    @Column(name = "meta_calorias", nullable = false)
    private int metaCalorias = 2000;

    @Column(name = "meta_exercicio_min", nullable = false)
    private int metaExercicioMin = 30;

    @Column(name = "meta_calorias_gastas", nullable = false)
    private int metaCaloriasGastas = 500;
}
