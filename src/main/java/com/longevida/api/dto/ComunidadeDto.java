package com.longevida.api.dto;

import com.longevida.api.entity.NivelTreino;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ComunidadeDto {

    private ComunidadeDto() {}

    public record DietaResumo(Long id, String nome, Integer calorias) {}

    public record TreinoResumo(Long id, String nome, NivelTreino nivel) {}

    /** Perfil profissional como aparece na comunidade. */
    public record ProfissionalResposta(
            Long id,
            String nome,
            String especialidade,
            String descricao,
            long seguidores,
            String foto,
            boolean seguindo,
            boolean ehVoce,
            List<DietaResumo> dietas,
            List<TreinoResumo> treinos) {}

    /** Perfil profissional do próprio usuário (tela de edição). */
    public record MeuPerfilResposta(Long id, String especialidade, String registro, String descricao, long seguidores) {}

    public record MeuPerfilRequest(
            @NotBlank(message = "Informe a especialidade.") @Size(max = 60, message = "Especialidade muito longa.") String especialidade,
            @Size(max = 500, message = "Descrição muito longa.") String descricao) {}
}
