package com.longevida.api.repository;

import com.longevida.api.entity.Seguidor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    Optional<Seguidor> findBySeguidorIdAndProfissionalId(Long seguidorId, Long profissionalId);

    long countByProfissionalId(Long profissionalId);

    interface ContagemSeguidores {
        Long getProfissionalId();

        Long getTotal();
    }

    @Query("select s.profissional.id as profissionalId, count(s) as total from Seguidor s "
            + "where s.profissional.id in :ids group by s.profissional.id")
    List<ContagemSeguidores> contarPorProfissional(@Param("ids") Collection<Long> ids);

    /** Ids dos perfis profissionais que o usuário segue. */
    @Query("select s.profissional.id from Seguidor s where s.seguidor.id = :usuarioId")
    Set<Long> idsSeguidosPor(@Param("usuarioId") Long usuarioId);

    /** Ids dos usuários que seguem o perfil. */
    @Query("select s.seguidor.id from Seguidor s where s.profissional.id = :profissionalId")
    List<Long> idsDosSeguidores(@Param("profissionalId") Long profissionalId);
}
