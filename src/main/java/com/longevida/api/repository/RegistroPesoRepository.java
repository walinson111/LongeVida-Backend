package com.longevida.api.repository;

import com.longevida.api.entity.RegistroPeso;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroPesoRepository extends JpaRepository<RegistroPeso, Long> {

    Optional<RegistroPeso> findByUsuarioIdAndData(Long usuarioId, LocalDate data);

    List<RegistroPeso> findByUsuarioIdAndDataBetweenOrderByDataAsc(Long usuarioId, LocalDate de, LocalDate ate);

    Optional<RegistroPeso> findFirstByUsuarioIdOrderByDataDesc(Long usuarioId);
}
