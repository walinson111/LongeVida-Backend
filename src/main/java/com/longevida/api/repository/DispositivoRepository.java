package com.longevida.api.repository;

import com.longevida.api.entity.Dispositivo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    List<Dispositivo> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);

    Optional<Dispositivo> findByIdAndUsuarioId(Long id, Long usuarioId);
}
