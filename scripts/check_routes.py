#!/usr/bin/env python3
"""Confere se toda chamada HTTP do frontend (services/endpoints.ts) existe num controller Spring.
Uso (na raiz do repositório): python3 backend/scripts/check_routes.py"""
import re, glob, os, sys

raiz = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
norm = lambda p: re.sub(r'\{[^}]+\}', '{x}', p.rstrip('/'))

java = set()
for path in glob.glob(os.path.join(raiz, 'backend/src/main/java/com/longevida/api/controller/*.java')):
    src = open(path, encoding='utf-8').read()
    base = re.search(r'@RequestMapping\("([^"]*)"\)', src)
    base = base.group(1) if base else ''
    for m in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?', src):
        java.add((m.group(1).upper(), norm(base + (m.group(2) or ''))))

ts = open(os.path.join(raiz, 'frontend/src/services/endpoints.ts'), encoding='utf-8').read()
usados = set()
for m in re.finditer(r'api\.(get|post|postSemCorpo|put|patch|delete)(?:<[^(]*?>)?\(\s*([`\'"])(.*?)\2', ts):
    metodo = {'postSemCorpo': 'POST'}.get(m.group(1), m.group(1).upper())
    caminho = re.sub(r'\$\{[^}]*\}(?=/|$)', '{x}', m.group(3))   # ${id} entre barras
    caminho = caminho.split('${')[0].split('?')[0]                # sufixos opcionais (?busca=)
    usados.add((metodo, norm(caminho)))

erros = 0
for metodo, caminho in sorted(usados):
    if (metodo, caminho) not in java:
        print(f'ERRO o frontend chama {metodo} {caminho}, que não existe no backend'); erros += 1
print(f'{len(usados)} rotas do frontend verificadas contra {len(java)} rotas do backend')
sem_uso = sorted(java - usados)
print('rotas do backend que o app ainda não usa (ok, são da API):', ', '.join(f'{m} {p}' for m, p in sem_uso))
print('OK: todas as chamadas do frontend existem no backend' if not erros else f'{erros} divergência(s)')
sys.exit(1 if erros else 0)
