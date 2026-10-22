package com.longevida.api.repository;

import com.longevida.api.entity.ConfiguracaoUsuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfiguracaoUsuarioRepository extends JpaRepository<ConfiguracaoUsuario, Long> {

    Optional<ConfiguracaoUsuario> findByUsuarioId(Long usuarioId);

    /** Dos usuários informados, devolve os ids dos que querem receber notificações. */
    @Query("select c.usuario.id from ConfiguracaoUsuario c "
            + "where c.usuario.id in :ids and c.notificacoesAtivas = true")
    List<Long> idsComNotificacoesAtivas(@Param("ids") Collection<Long> ids);
}
