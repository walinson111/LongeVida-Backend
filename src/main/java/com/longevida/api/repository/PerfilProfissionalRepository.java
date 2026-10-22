package com.longevida.api.repository;

import com.longevida.api.entity.PerfilProfissional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilProfissionalRepository extends JpaRepository<PerfilProfissional, Long> {

    Optional<PerfilProfissional> findByUsuarioId(Long usuarioId);

    @Query("select p from PerfilProfissional p join fetch p.usuario order by p.id")
    List<PerfilProfissional> listarComUsuario();

    @Query("select p from PerfilProfissional p join fetch p.usuario where p.id = :id")
    Optional<PerfilProfissional> buscarComUsuario(@Param("id") Long id);
}
