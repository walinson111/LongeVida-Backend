package com.longevida.api.repository;

import com.longevida.api.entity.MedicaoWearable;
import com.longevida.api.entity.TipoMedicao;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicaoWearableRepository extends JpaRepository<MedicaoWearable, Long> {

    boolean existsByDispositivoIdAndTipoAndMedidoEm(Long dispositivoId, TipoMedicao tipo, Instant medidoEm);

    Optional<MedicaoWearable> findFirstByUsuarioIdAndTipoOrderByMedidoEmDesc(Long usuarioId, TipoMedicao tipo);
}
