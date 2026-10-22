\set ON_ERROR_STOP on
CREATE OR REPLACE FUNCTION expect_fail(label text, stmt text) RETURNS void AS $$
BEGIN
  BEGIN
    EXECUTE stmt;
    RAISE EXCEPTION 'FALHOU O TESTE (deveria ter dado erro): %', label;
  EXCEPTION WHEN check_violation OR unique_violation OR foreign_key_violation OR not_null_violation THEN
    RAISE NOTICE 'OK  (rejeitado) %', label;
  END;
END $$ LANGUAGE plpgsql;

INSERT INTO usuarios (nome,email,senha_hash,idade) VALUES ('Maria','Maria@Ex.com','x',65),('Joao','joao@ex.com','x',40);
INSERT INTO configuracoes_usuario (usuario_id) SELECT id FROM usuarios;
INSERT INTO perfis_profissionais (usuario_id,especialidade,registro_profissional) SELECT id,'Nutricionista','CRN-1' FROM usuarios WHERE nome='Joao';

SELECT expect_fail('e-mail duplicado ignorando caixa', $$INSERT INTO usuarios (nome,email,senha_hash,idade) VALUES ('X','MARIA@ex.com','x',30)$$);
SELECT expect_fail('idade 0', $$INSERT INTO usuarios (nome,email,senha_hash,idade) VALUES ('X','a@a.com','x',0)$$);
SELECT expect_fail('peso 600', $$UPDATE usuarios SET peso_kg=600 WHERE nome='Maria'$$);
SELECT expect_fail('nivel inválido', $$UPDATE usuarios SET nivel_atividade='ZZZ' WHERE nome='Maria'$$);
SELECT expect_fail('meta água 0', $$UPDATE configuracoes_usuario SET meta_agua_ml=0$$);
SELECT expect_fail('configuração duplicada p/ usuário', $$INSERT INTO configuracoes_usuario (usuario_id) SELECT id FROM usuarios LIMIT 1$$);
SELECT expect_fail('segundo perfil profissional do mesmo usuário', $$INSERT INTO perfis_profissionais (usuario_id,especialidade,registro_profissional) SELECT id,'X','Y' FROM usuarios WHERE nome='Joao'$$);

INSERT INTO seguidores (seguidor_id, profissional_id) SELECT u.id, p.id FROM usuarios u, perfis_profissionais p WHERE u.nome='Maria';
SELECT expect_fail('seguir duas vezes', $$INSERT INTO seguidores (seguidor_id, profissional_id) SELECT u.id, p.id FROM usuarios u, perfis_profissionais p WHERE u.nome='Maria'$$);

INSERT INTO dietas (usuario_id,nome,calorias) SELECT id,'D1',2000 FROM usuarios WHERE nome='Maria';
SELECT expect_fail('dieta calorias 20000', $$INSERT INTO dietas (usuario_id,nome,calorias) SELECT id,'D2',20000 FROM usuarios LIMIT 1$$);
SELECT expect_fail('dieta status inválido', $$UPDATE dietas SET status='X'$$);

INSERT INTO treinos (usuario_id,nome,objetivo,nivel) SELECT id,'T1','Força','INICIANTE' FROM usuarios WHERE nome='Maria';
INSERT INTO exercicios (treino_id,ordem,nome,series,repeticoes) SELECT id,0,'Supino',4,12 FROM treinos;
SELECT expect_fail('exercício séries 0', $$INSERT INTO exercicios (treino_id,ordem,nome,series,repeticoes) SELECT id,1,'X',0,10 FROM treinos$$);
SELECT expect_fail('treino nível inválido', $$UPDATE treinos SET nivel='EXPERT'$$);

INSERT INTO registros_peso (usuario_id,data,peso_kg) SELECT id,'2026-10-19',73.8 FROM usuarios WHERE nome='Maria';
SELECT expect_fail('2 pesos no mesmo dia', $$INSERT INTO registros_peso (usuario_id,data,peso_kg) SELECT id,'2026-10-19',74 FROM usuarios WHERE nome='Maria'$$);
SELECT expect_fail('água 0 ml', $$INSERT INTO registros_agua (usuario_id,data,quantidade_ml) SELECT id,'2026-10-19',0 FROM usuarios LIMIT 1$$);

INSERT INTO lembretes (usuario_id,tipo,titulo,horario) SELECT id,'REFEICAO','Almoço','12:00' FROM usuarios WHERE nome='Maria';
INSERT INTO notificacoes (usuario_id,tipo,titulo,mensagem,lembrete_id,referencia_data) SELECT usuario_id,'LEMBRETE','Almoço','Hora!',id,'2026-10-20' FROM lembretes;
SELECT expect_fail('lembrete notificado 2x no mesmo dia', $$INSERT INTO notificacoes (usuario_id,tipo,titulo,mensagem,lembrete_id,referencia_data) SELECT usuario_id,'LEMBRETE','Almoço','Hora!',id,'2026-10-20' FROM lembretes$$);
INSERT INTO notificacoes (usuario_id,tipo,titulo,mensagem) SELECT id,'SOCIAL','a','b' FROM usuarios WHERE nome='Maria';
INSERT INTO notificacoes (usuario_id,tipo,titulo,mensagem) SELECT id,'SOCIAL','a','b' FROM usuarios WHERE nome='Maria';  -- sem lembrete: pode repetir

INSERT INTO dispositivos (usuario_id,nome,marca) SELECT id,'Mi Band 7','Xiaomi' FROM usuarios WHERE nome='Maria';
INSERT INTO medicoes_wearable (dispositivo_id,usuario_id,tipo,valor,medido_em) SELECT d.id,d.usuario_id,'PASSOS',1200,'2026-10-20T10:00:00Z' FROM dispositivos d;
SELECT expect_fail('medição duplicada', $$INSERT INTO medicoes_wearable (dispositivo_id,usuario_id,tipo,valor,medido_em) SELECT d.id,d.usuario_id,'PASSOS',1300,'2026-10-20T10:00:00Z' FROM dispositivos d$$);
SELECT expect_fail('tipo de medição inválido', $$INSERT INTO medicoes_wearable (dispositivo_id,usuario_id,tipo,valor,medido_em) SELECT d.id,d.usuario_id,'GLICOSE',1,now() FROM dispositivos d$$);

-- Importação: apagar a dieta original NÃO apaga a cópia; mantém o crédito.
INSERT INTO dietas (usuario_id,nome,calorias,status) SELECT id,'Publicada',1800,'PUBLICA' FROM usuarios WHERE nome='Joao';
INSERT INTO dietas (usuario_id,nome,calorias,origem_dieta_id,credito_nome) SELECT m.id,'Cópia',1800,d.id,'Joao' FROM usuarios m, dietas d WHERE m.nome='Maria' AND d.nome='Publicada';
DELETE FROM dietas WHERE nome='Publicada';
DO $$ BEGIN
  IF (SELECT origem_dieta_id IS NULL AND credito_nome='Joao' FROM dietas WHERE nome='Cópia') THEN RAISE NOTICE 'OK  cópia mantém crédito após original apagado';
  ELSE RAISE EXCEPTION 'cópia perdeu crédito'; END IF; END $$;

-- Cascata: apagar usuário remove todos os dados dele (LGPD / exclusão de conta).
DELETE FROM usuarios WHERE nome='Maria';
DO $$ DECLARE n int; BEGIN
  SELECT (SELECT count(*) FROM dietas)+(SELECT count(*) FROM treinos)+(SELECT count(*) FROM exercicios)+(SELECT count(*) FROM seguidores)
        +(SELECT count(*) FROM lembretes)+(SELECT count(*) FROM notificacoes)+(SELECT count(*) FROM dispositivos)+(SELECT count(*) FROM medicoes_wearable)
        +(SELECT count(*) FROM registros_peso)+(SELECT count(*) FROM configuracoes_usuario WHERE usuario_id NOT IN (SELECT id FROM usuarios)) INTO n;
  IF n = 0 THEN RAISE NOTICE 'OK  exclusão do usuário cascateia todos os dados'; ELSE RAISE EXCEPTION 'sobraram % linhas órfãs', n; END IF; END $$;
