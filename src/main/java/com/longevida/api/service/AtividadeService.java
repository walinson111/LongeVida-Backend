package com.longevida.api.service;

import com.longevida.api.config.AppProperties;
import com.longevida.api.dto.AtividadeDto;
import com.longevida.api.entity.Atividade;
import com.longevida.api.entity.Exercicio;
import com.longevida.api.entity.Treino;
import com.longevida.api.exception.RecursoNaoEncontradoException;
import com.longevida.api.exception.RegraNegocioException;
import com.longevida.api.repository.AtividadeRepository;
import com.longevida.api.repository.TreinoRepository;
import com.longevida.api.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtividadeService {

    private static final int MAX_DIAS = 366;

    private final AtividadeRepository atividadeRepository;
    private final TreinoRepository treinoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AppProperties props;
    private final Clock clock;

    public AtividadeService(AtividadeRepository atividadeRepository,
                            TreinoRepository treinoRepository,
                            UsuarioRepository usuarioRepository,
                            AppProperties props,
                            Clock clock) {
        this.atividadeRepository = atividadeRepository;
        this.treinoRepository = treinoRepository;
        this.usuarioRepository = usuarioRepository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public AtividadeDto.Resposta registrar(Long usuarioId, AtividadeDto.Request req) {
        // O treino precisa ser do próprio usuário.
        Treino treino = treinoRepository.findByIdAndUsuarioId(req.treinoId(), usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Treino não encontrado."));

        Set<Long> idsDoTreino = treino.getExercicios().stream().map(Exercicio::getId).collect(Collectors.toSet());
        Set<Long> concluidos = new HashSet<>(req.exerciciosConcluidosIds());
        if (!idsDoTreino.containsAll(concluidos)) {
            throw new RegraNegocioException("Há exercícios que não pertencem ao treino selecionado.");
        }

        Atividade a = new Atividade();
        a.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        a.setTreino(treino);
        a.setNome(treino.getNome());
        a.setDataAtividade(req.data() != null ? req.data() : LocalDate.now(clock));
        a.setHorario(req.horario() != null ? req.horario() : LocalTime.now(clock).truncatedTo(ChronoUnit.MINUTES));
        a.setDuracaoMin(req.duracaoMin());
        a.setExerciciosConcluidos(concluidos.size());
        a.setExerciciosTotal(idsDoTreino.size());
        // Estimativa simples (kcal por exercício concluído), herdada do protótipo. Ver README.
        a.setCaloriasGastas(concluidos.size() * props.atividade().kcalPorExercicio());
        return paraDto(atividadeRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<AtividadeDto.Resposta> listar(Long usuarioId, LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : LocalDate.now(clock);
        LocalDate inicio = de != null ? de : fim;
        if (inicio.isAfter(fim) || ChronoUnit.DAYS.between(inicio, fim) > MAX_DIAS) {
            throw new RegraNegocioException("Período inválido (máximo de " + MAX_DIAS + " dias).");
        }
        return atividadeRepository
                .findByUsuarioIdAndDataAtividadeBetweenOrderByDataAtividadeDescHorarioDesc(usuarioId, inicio, fim)
                .stream().map(AtividadeService::paraDto).toList();
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        Atividade a = atividadeRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
        atividadeRepository.delete(a);
    }

    private static AtividadeDto.Resposta paraDto(Atividade a) {
        return new AtividadeDto.Resposta(a.getId(), a.getTreino() != null ? a.getTreino().getId() : null, a.getNome(),
                a.getDataAtividade(), a.getHorario(), a.getDuracaoMin(), a.getCaloriasGastas(),
                a.getExerciciosConcluidos(), a.getExerciciosTotal());
    }
}
