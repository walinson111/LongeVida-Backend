package com.longevida.api.repository;

import com.longevida.api.entity.Atividade;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    List<Atividade> findByUsuarioIdAndDataAtividadeBetweenOrderByDataAtividadeDescHorarioDesc(
            Long usuarioId, LocalDate de, LocalDate ate);

    Optional<Atividade> findByIdAndUsuarioId(Long id, Long usuarioId);

    interface TotalDia {
        LocalDate getData();

        Long getCalorias();

        Long getMinutos();
    }

    @Query("select a.dataAtividade as data, sum(a.caloriasGastas) as calorias, sum(a.duracaoMin) as minutos "
            + "from Atividade a "
            + "where a.usuario.id = :usuarioId and a.dataAtividade between :de and :ate "
            + "group by a.dataAtividade")
    List<TotalDia> totaisPorDia(@Param("usuarioId") Long usuarioId,
                                @Param("de") LocalDate de,
                                @Param("ate") LocalDate ate);
}
