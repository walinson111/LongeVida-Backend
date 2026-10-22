package com.longevida.api.repository;

import com.longevida.api.entity.Alimento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlimentoRepository extends JpaRepository<Alimento, Long> {

    List<Alimento> findAllByOrderByNomeAsc();

    List<Alimento> findByNomeContainingIgnoreCaseOrderByNomeAsc(String trecho);
}
