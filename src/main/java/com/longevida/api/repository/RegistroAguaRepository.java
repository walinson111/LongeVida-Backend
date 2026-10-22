package com.longevida.api.repository;

import com.longevida.api.entity.RegistroAgua;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroAguaRepository extends JpaRepository<RegistroAgua, Long> {

    @Query("select coalesce(sum(r.quantidadeMl), 0) from RegistroAgua r "
            + "where r.usuario.id = :usuarioId and r.data = :data")
    long totalDoDia(@Param("usuarioId") Long usuarioId, @Param("data") LocalDate data);
}
