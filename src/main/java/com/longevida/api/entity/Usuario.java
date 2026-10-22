package com.longevida.api.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "email", nullable = false, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(name = "idade", nullable = false)
    private Integer idade;

    @Column(name = "peso_kg", precision = 5, scale = 1)
    private BigDecimal pesoKg;

    @Column(name = "altura_cm")
    private Integer alturaCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_atividade", length = 20)
    private NivelAtividade nivelAtividade;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "usuario_restricoes", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "restricao", nullable = false, length = 20)
    private Set<Restricao> restricoes = new HashSet<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /** O perfil de saúde (primeiro acesso) está completo quando peso, altura e nível foram informados. */
    public boolean isPerfilSaudeCompleto() {
        return pesoKg != null && alturaCm != null && nivelAtividade != null;
    }

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
