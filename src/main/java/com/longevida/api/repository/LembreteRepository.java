package com.longevida.api.repository;

import com.longevida.api.entity.Lembrete;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LembreteRepository extends JpaRepository<Lembrete, Long> {

    List<Lembrete> findByUsuarioIdOrderByHorarioAsc(Long usuarioId);

    Optional<Lembrete> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    /** Lembretes ativos para o horário informado, de usuários com notificações ligadas. */
    @Query("select l from Lembrete l join fetch l.usuario "
            + "where l.ativo = true and l.horario = :horario "
            + "and l.usuario.id in (select c.usuario.id from ConfiguracaoUsuario c where c.notificacoesAtivas = true)")
    List<Lembrete> listarAtivosNoHorario(@Param("horario") LocalTime horario);
}
