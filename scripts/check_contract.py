#!/usr/bin/env python3
"""Confere se os tipos do frontend (frontend/src/services/types.ts) batem com os DTOs Java.
Uso (na raiz do repositório): python3 backend/scripts/check_contract.py"""
import re, sys, glob, os

raiz = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ts = open(os.path.join(raiz, 'frontend/src/services/types.ts'), encoding='utf-8').read()

# --- records Java: "Holder.Record" -> {componentes}
records = {}
for path in glob.glob(os.path.join(raiz, 'backend/src/main/java/com/longevida/api/dto/*.java')):
    src = open(path, encoding='utf-8').read()
    holder = re.search(r'public final class (\w+)', src).group(1)
    for m in re.finditer(r'public record (\w+)\((.*?)\)\s*\{', src, re.S):
        corpo = re.sub(r'@\w+(\([^)]*(\([^)]*\))?[^)]*\))?', '', m.group(2))   # remove anotações
        corpo = re.sub(r'/\*\*.*?\*/', '', corpo, flags=re.S)
        comps, nivel, atual = [], 0, ''
        for ch in corpo:
            if ch == '<': nivel += 1
            if ch == '>': nivel -= 1
            if ch == ',' and nivel == 0:
                comps.append(atual); atual = ''
            else:
                atual += ch
        comps.append(atual)
        nomes = {c.split()[-1] for c in comps if c.strip()}
        # @JsonProperty("isProfissional") renomeia o componente "profissional"
        if 'JsonProperty("isProfissional")' in m.group(2):
            nomes.discard('profissional'); nomes.add('isProfissional')
        records[f'{holder}.{m.group(1)}'] = nomes

# --- interfaces TS
interfaces = {}
for m in re.finditer(r'export interface (\w+) \{(.*?)\n\}', ts, re.S):
    campos = set(re.findall(r'^\s{2}(\w+)\??:', m.group(2), re.M))
    interfaces[m.group(1)] = campos

# TS -> (record Java, direção): "resp" = o TS lê o que o Java envia; "req" = o TS envia o que o Java recebe
mapa = {
    'Usuario': ('UsuarioDto.Resposta', 'resp'), 'AuthResponse': ('AuthDto.AuthResponse', 'resp'),
    'CadastroRequest': ('AuthDto.CadastroRequest', 'req'), 'AtualizarUsuarioRequest': ('UsuarioDto.AtualizarRequest', 'req'),
    'Configuracao': ('UsuarioDto.ConfiguracaoResposta', 'resp'),
    'Dieta': ('DietaDto.Resposta', 'resp'), 'DietaRequest': ('DietaDto.Request', 'req'),
    'Autor': ('DietaDto.AutorResumo', 'resp'), 'DietaPublica': ('DietaDto.PublicaResposta', 'resp'),
    'Exercicio': ('TreinoDto.ExercicioResposta', 'resp'), 'Treino': ('TreinoDto.Resposta', 'resp'),
    'TreinoRequest': ('TreinoDto.Request', 'req'), 'TreinoPublico': ('TreinoDto.PublicaResposta', 'resp'),
    'Alimento': ('AlimentoDto.Resposta', 'resp'), 'RefeicaoRequest': ('RefeicaoDto.Request', 'req'),
    'Refeicao': ('RefeicaoDto.Resposta', 'resp'), 'AtividadeRequest': ('AtividadeDto.Request', 'req'),
    'Atividade': ('AtividadeDto.Resposta', 'resp'), 'Metas': ('ProgressoDto.Metas', 'resp'),
    'ResumoDiario': ('ProgressoDto.ResumoDiario', 'resp'), 'ItemHistorico': ('ProgressoDto.ItemHistorico', 'resp'),
    'Progresso': ('ProgressoDto.Progresso', 'resp'), 'Profissional': ('ComunidadeDto.ProfissionalResposta', 'resp'),
    'MeuPerfilProfissional': ('ComunidadeDto.MeuPerfilResposta', 'resp'),
    'Notificacao': ('NotificacaoDto.Resposta', 'resp'), 'Lembrete': ('NotificacaoDto.LembreteResposta', 'resp'),
    'Dispositivo': ('DispositivoDto.Resposta', 'resp'), 'MedicaoWearable': ('DispositivoDto.MedicaoResposta', 'resp'),
}
erros = 0
for nome_ts, (rec, _) in mapa.items():
    if nome_ts not in interfaces: print(f'ERRO interface {nome_ts} ausente no TS'); erros += 1; continue
    if rec not in records: print(f'ERRO record {rec} ausente no Java'); erros += 1; continue
    faltam_no_java = interfaces[nome_ts] - records[rec]
    if faltam_no_java:
        print(f'ERRO {nome_ts}: campos do TS que o Java {rec} não tem: {sorted(faltam_no_java)}'); erros += 1
# campos Java de respostas que o TS ignora (informativo)
for nome_ts, (rec, direcao) in mapa.items():
    if direcao == 'resp' and nome_ts in interfaces and rec in records:
        extras = records[rec] - interfaces[nome_ts]
        if extras: print(f'info {rec}: o app não usa {sorted(extras)}')
print('OK: contrato frontend x backend consistente' if not erros else f'{erros} divergência(s)')
sys.exit(1 if erros else 0)
