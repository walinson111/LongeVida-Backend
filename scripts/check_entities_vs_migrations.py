#!/usr/bin/env python3
"""Confere se todo @Column/@JoinColumn das entidades JPA existe nas migrações Flyway.
Uso: python3 scripts/check_entities_vs_migrations.py   (na pasta backend)
O Hibernate roda com ddl-auto=validate, então divergências derrubariam a aplicação na subida."""
import re, glob, sys

sql = ''.join(open(f, encoding='utf-8').read() for f in sorted(glob.glob('src/main/resources/db/migration/V*.sql')))
tables = {}
for m in re.finditer(r'CREATE TABLE (\w+) \((.*?)\n\);', sql, re.S):
    cols = set()
    for line in m.group(2).split('\n'):
        line = line.strip()
        mm = re.match(r'(\w+)\s+(BIGINT|VARCHAR|INTEGER|NUMERIC|BOOLEAN|TIMESTAMPTZ|DATE|TIME)\b', line)
        if mm:
            cols.add(mm.group(1))
    tables[m.group(1)] = cols

erros = 0
for path in sorted(glob.glob('src/main/java/com/longevida/api/entity/*.java')):
    src = open(path, encoding='utf-8').read()
    t = re.search(r'@Table\(name = "(\w+)"\)', src)
    if not t:
        continue
    nome = t.group(1)
    if nome not in tables:
        print(f'ERRO {path}: tabela {nome} não existe nas migrações'); erros += 1; continue
    usados = re.findall(r'@(?:Column|JoinColumn)\(name = "(\w+)"', src)
    # coleção de elementos (tabela própria)
    ct = re.search(r'@CollectionTable\(name = "(\w+)"', src)
    for c in usados:
        alvo = nome
        if ct and c in ('restricao', 'usuario_id') and c not in tables.get(nome, set()) or (ct and c == 'restricao'):
            alvo = ct.group(1)
        if c not in tables.get(alvo, set()):
            print(f'ERRO {path}: coluna {c} não existe em {alvo}'); erros += 1
    # colunas NOT NULL sem default no SQL que a entidade não mapeia
    for m in re.finditer(r'CREATE TABLE ' + nome + r' \((.*?)\n\);', sql, re.S):
        for line in m.group(1).split('\n'):
            mm = re.match(r'\s*(\w+)\s+\w+.*NOT NULL', line)
            if mm and 'DEFAULT' not in line and 'PRIMARY KEY' not in line and mm.group(1) not in usados and mm.group(1) != 'id':
                print(f'AVISO {path}: coluna NOT NULL sem default "{mm.group(1)}" não mapeada'); 
print('OK: entidades e migrações consistentes' if not erros else f'{erros} erro(s)')
sys.exit(1 if erros else 0)
