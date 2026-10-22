package com.longevida.api.repository;

import com.longevida.api.entity.Treino;
import com.longevida.api.entity.Visibilidade;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TreinoRepository extends JpaRepository<Treino, Long> {

    @EntityGraph(attributePaths = "exercicios")
    List<Treino> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    @EntityGraph(attributePaths = "exercicios")
    Optional<Treino> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("select t from Treino t join fetch t.usuario left join fetch t.exercicios "
            + "where t.id = :id and t.status = :status")
    Optional<Treino> buscarPublico(@Param("id") Long id, @Param("status") Visibilidade status);

    @Query("select t from Treino t join fetch t.usuario "
            + "where t.usuario.id in :usuarioIds and t.status = :status order by t.criadoEm desc")
    List<Treino> listarPorUsuariosEStatus(@Param("usuarioIds") Collection<Long> usuarioIds,
                                          @Param("status") Visibilidade status);
}
