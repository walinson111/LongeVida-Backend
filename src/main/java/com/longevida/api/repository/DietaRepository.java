package com.longevida.api.repository;

import com.longevida.api.entity.Dieta;
import com.longevida.api.entity.Visibilidade;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DietaRepository extends JpaRepository<Dieta, Long> {

    List<Dieta> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    /** Busca restrita ao dono: dieta de outro usuário simplesmente "não existe". */
    Optional<Dieta> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("select d from Dieta d join fetch d.usuario where d.id = :id and d.status = :status")
    Optional<Dieta> buscarPublica(@Param("id") Long id, @Param("status") Visibilidade status);

    @Query("select d from Dieta d join fetch d.usuario "
            + "where d.usuario.id in :usuarioIds and d.status = :status order by d.criadoEm desc")
    List<Dieta> listarPorUsuariosEStatus(@Param("usuarioIds") Collection<Long> usuarioIds,
                                         @Param("status") Visibilidade status);
}
