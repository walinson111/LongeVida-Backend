package com.longevida.api.repository;

import com.longevida.api.entity.Refeicao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefeicaoRepository extends JpaRepository<Refeicao, Long> {

    @EntityGraph(attributePaths = "itens")
    List<Refeicao> findByUsuarioIdAndDataRefeicaoBetweenOrderByDataRefeicaoDescHorarioDesc(
            Long usuarioId, LocalDate de, LocalDate ate);

    @EntityGraph(attributePaths = "itens")
    Optional<Refeicao> findByIdAndUsuarioId(Long id, Long usuarioId);

    interface TotalDia {
        LocalDate getData();

        Long getCalorias();

        BigDecimal getProteinas();

        BigDecimal getCarboidratos();

        BigDecimal getGorduras();
    }

    @Query("select r.dataRefeicao as data, sum(i.calorias) as calorias, sum(i.proteinas) as proteinas, "
            + "sum(i.carboidratos) as carboidratos, sum(i.gorduras) as gorduras "
            + "from Refeicao r join r.itens i "
            + "where r.usuario.id = :usuarioId and r.dataRefeicao between :de and :ate "
            + "group by r.dataRefeicao")
    List<TotalDia> totaisPorDia(@Param("usuarioId") Long usuarioId,
                                @Param("de") LocalDate de,
                                @Param("ate") LocalDate ate);
}
