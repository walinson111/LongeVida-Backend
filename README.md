# LongeVida — Como rodar o Backend e usar com o Frontend

Este guia explica como colocar a **API (Spring Boot + PostgreSQL)** no ar e conectar o **app mobile (React Native / Expo)** a ela.

| Repositório | Tecnologia | Link |
| --- | --- | --- |
| Backend | Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway, JWT | https://github.com/walinson111/LongeVida-Backend |
| Frontend | React Native, Expo, expo-router, TypeScript | https://github.com/Nicolas-Augusto-Almeida/LongeVida-Frontend |

```
┌──────────────────────┐   HTTP + JWT (Bearer)   ┌────────────────────────┐   JDBC   ┌──────────────┐
│  App Expo (frontend) │ ──────────────────────► │  API Spring (porta 8080)│ ───────► │ PostgreSQL   │
│  celular / emulador  │ ◄────────────────────── │  /api/**               │          │ (porta 5432) │
└──────────────────────┘          JSON           └────────────────────────┘          └──────────────┘
```

---

## 1. Pré-requisitos

| Ferramenta | Versão | Para quê |
| --- | --- | --- |
| **JDK** | 21 | Rodar o backend |
| **Maven** | 3.9+ | Compilar/rodar o backend (o repositório **não** tem `mvnw`) |
| **Docker** + Docker Compose | qualquer recente | Subir o PostgreSQL (opcional — pode instalar o Postgres direto na máquina) |
| **Node.js** | 20+ | Rodar o frontend |
| **Expo Go** (no celular) ou emulador Android/iOS | — | Abrir o app |

Confira:

```bash
java -version     # deve mostrar 21
mvn -version
docker --version
node -v
```

---

## 2. Clonar os dois projetos

Recomendado deixar os dois lado a lado numa mesma pasta:

```bash
mkdir longevida && cd longevida
git clone https://github.com/walinson111/LongeVida-Backend.git backend
git clone https://github.com/Nicolas-Augusto-Almeida/LongeVida-Frontend.git frontend
```

```
longevida/
├── backend/    # API Spring Boot
└── frontend/   # App Expo
```

> Os scripts em `backend/scripts/check_contract.py` e `check_routes.py` esperam exatamente essa estrutura (`backend/` e `frontend/` na mesma pasta). Veja a seção 6.

---

## 3. Rodando o Backend

### 3.1. Variáveis de ambiente

O backend **não tem nenhuma credencial no código**: tudo vem de variáveis de ambiente (`src/main/resources/application.yml`).

| Variável | Obrigatória? | Padrão | Descrição |
| --- | --- | --- | --- |
| `DB_PASSWORD` | **Sim** | — | Senha do usuário do PostgreSQL |
| `JWT_SECRET` | **Sim** | — | Chave do JWT em **Base64**, com pelo menos 32 bytes |
| `DB_URL` | Não | `jdbc:postgresql://localhost:5432/longevida` | URL JDBC do banco |
| `DB_USERNAME` | Não | `longevida` | Usuário do banco |
| `PORT` | Não | `8080` | Porta da API |
| `CORS_ALLOWED_ORIGINS` | Não | `http://localhost:8081,http://localhost:19006` | Origens liberadas (só importa para o app rodando **no navegador**) |
| `JWT_EXPIRATION_MINUTES` | Não | `10080` (7 dias) | Validade do token |
| `APP_TIMEZONE` | Não | `America/Sao_Paulo` | Fuso usado nos resumos diários e lembretes |
| `MAIL_ENABLED` | Não | `false` | `true` envia e-mails reais (exige `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`) |
| `MAIL_FROM` | Não | `no-reply@longevida.local` | Remetente dos e-mails |
| `SPRING_PROFILES_ACTIVE` | Não | — | Use `dev` localmente (ver 3.4) |

**Gerar um `JWT_SECRET` válido:**

```bash
# Linux / macOS / Git Bash
openssl rand -base64 48
```

```powershell
# Windows PowerShell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

### 3.2. Criar o arquivo `backend/.env`

Crie `backend/.env` (ele já está no `.gitignore`, **nunca faça commit dele**):

```env
DB_USERNAME=longevida
DB_PASSWORD=troque-esta-senha
JWT_SECRET=COLE_AQUI_O_VALOR_GERADO
SPRING_PROFILES_ACTIVE=dev
```

> ⚠️ O Docker Compose lê o `.env` sozinho, mas o **Spring Boot não**. Por isso, no passo 3.4 as variáveis são carregadas no terminal antes de rodar a API.

### 3.3. Subir o PostgreSQL

**Opção A — Docker (recomendado):**

```bash
cd backend
docker compose up -d
```

Isso cria um Postgres 16 na porta `5432`, com banco `longevida`, usuário `DB_USERNAME` e senha `DB_PASSWORD` do `.env`. Os dados ficam no volume `longevida-db`.

**Opção B — PostgreSQL instalado na máquina:**

```sql
CREATE USER longevida WITH PASSWORD 'troque-esta-senha';
CREATE DATABASE longevida OWNER longevida;
```

> Não é preciso criar tabelas: o **Flyway** roda as migrações (`src/main/resources/db/migration/V1…V6`) automaticamente na primeira subida, incluindo uma lista inicial de alimentos.

### 3.4. Iniciar a API

Dentro de `backend/`:

**Linux / macOS / Git Bash:**

```bash
set -a && source .env && set +a
mvn spring-boot:run
```

**Windows PowerShell:**

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $k, $v = $_ -split '=', 2; Set-Item -Path "env:$($k.Trim())" -Value $v.Trim()
}
mvn spring-boot:run
```

**IntelliJ / VS Code:** rode `LongeVidaApplication` e cadastre as mesmas variáveis em *Run Configuration → Environment variables* (ou use o plugin EnvFile apontando para o `.env`).

Sobre o perfil **`dev`**: com o envio de e-mail desativado, o código de **recuperação de senha** é impresso no log da API (linha `[DEV] E-mail desativado. Código de recuperação...`). Assim dá para testar o fluxo "Esqueci minha senha" sem configurar SMTP. **Não use `dev` em produção.**

### 3.5. Verificar se está no ar

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

Teste rápido de cadastro e login:

```bash
curl -X POST http://localhost:8080/api/auth/cadastro \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria","email":"maria@exemplo.com","senha":"Senha@123","idade":65}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@exemplo.com","senha":"Senha@123"}'
```

O login devolve um `token`. Todas as rotas fora de `/api/auth/**` e `/actuator/health` exigem o cabeçalho:

```
Authorization: Bearer <token>
```

> **Campos obrigatórios no cadastro:** `nome` (2–120), `email`, `senha` e `idade` (1–120).
>
> **Regras de senha:** 8 a 64 caracteres, com letra maiúscula, letra minúscula, número e caractere especial, sem espaços.
>
> **Recuperação de senha:** o código tem 6 dígitos e vale 15 minutos.

### 3.6. Rotas principais da API

| Recurso | Base | Observação |
| --- | --- | --- |
| Autenticação | `/api/auth` | `cadastro`, `login`, `esqueci-senha`, `redefinir-senha` (públicas) |
| Usuário | `/api/usuarios/me` | perfil e `/configuracoes` (metas) |
| Dietas | `/api/dietas` | CRUD + `publicar` / `despublicar` |
| Treinos | `/api/treinos` | CRUD + `publicar` / `despublicar` |
| Alimentos | `/api/alimentos` | busca de alimentos |
| Refeições | `/api/refeicoes` | registro de refeições |
| Atividades | `/api/atividades` | registro de atividade física |
| Progresso | `/api/resumo-diario`, `/api/historico`, `/api/progresso`, `/api/progresso/peso`, `/api/agua` | |
| Comunidade | `/api/comunidade` | profissionais, seguir, importar dietas/treinos |
| Perfil profissional | `/api/profissionais/me` | |
| Lembretes | `/api/lembretes` | |
| Notificações | `/api/notificacoes` | `nao-lidas`, `lida`, `marcar-todas-lidas` |
| Wearables | `/api/dispositivos` | conexão e medições |

### 3.7. Testes do backend

```bash
mvn test
```

Os testes de integração usam **Testcontainers**, então o **Docker precisa estar rodando** (eles sobem um Postgres próprio, sem usar o seu `.env`).

---

## 4. Rodando o Frontend

### 4.1. Instalar dependências

```bash
cd frontend
npm install
```

### 4.2. Apontar o app para o backend

A URL da API é definida em `src/screens/services/config.ts`:

- Se existir a variável **`EXPO_PUBLIC_API_URL`**, ela é usada.
- Senão, o padrão é `http://10.0.2.2:8080` no **emulador Android** e `http://localhost:8080` no resto (iOS Simulator, navegador).

Escolha conforme onde o app vai rodar:

| Onde o app roda | Precisa configurar? | Valor de `EXPO_PUBLIC_API_URL` |
| --- | --- | --- |
| Emulador Android | Não | (padrão `http://10.0.2.2:8080`) |
| Simulador iOS (Mac) | Não | (padrão `http://localhost:8080`) |
| Navegador (`npm run web`) | Não | (padrão `http://localhost:8080`) |
| **Celular físico com Expo Go** | **Sim** | `http://<IP-DO-SEU-PC>:8080` |

**Para celular físico** (caso mais comum), crie `frontend/.env`:

```env
EXPO_PUBLIC_API_URL=http://192.168.0.10:8080
```

Troque `192.168.0.10` pelo IP do seu computador na rede local:

- Windows: `ipconfig` → "Endereço IPv4"
- macOS: `ipconfig getifaddr en0`
- Linux: `hostname -I`

> O celular e o computador precisam estar **na mesma rede Wi-Fi**. `localhost` no celular aponta para o próprio celular, não para o PC.

### 4.3. Iniciar o app

```bash
npx expo start
```

- **Celular:** escaneie o QR code com o Expo Go (Android) ou a câmera (iOS).
- **Emulador Android:** pressione `a`.
- **Simulador iOS:** pressione `i`.
- **Navegador:** pressione `w` (ou `npm run web`).

Depois de alterar o `.env`, reinicie limpando o cache para a variável ser lida:

```bash
npx expo start -c
```

### 4.4. Primeiro uso

1. Abra o app e toque em **Cadastrar**.
2. Crie a conta (respeitando as regras de senha da seção 3.5).
3. Faça login — o app guarda o token JWT no dispositivo e o envia em todas as requisições.
4. Para testar **recuperação de senha**, peça o código no app e copie-o do **log da API** (perfil `dev`).

---

## 5. Resumo: subir tudo do zero

```bash
# Terminal 1 — banco + API
cd backend
docker compose up -d
set -a && source .env && set +a
mvn spring-boot:run

# Terminal 2 — app
cd frontend
npm install
npx expo start
```

---

## 6. Verificar o contrato entre Front e Back (opcional)

Na pasta **raiz** (a que contém `backend/` e `frontend/`):

```bash
python3 backend/scripts/check_routes.py     # toda chamada do app existe num controller?
python3 backend/scripts/check_contract.py   # os tipos TypeScript batem com os DTOs Java?
```

Dentro de `backend/`:

```bash
python3 scripts/check_entities_vs_migrations.py   # entidades JPA batem com as migrações?
```

> ⚠️ Os scripts de contrato procuram `frontend/src/services/...`, mas no repositório do frontend os arquivos estão em `frontend/src/screens/services/`. Se o script falhar com "arquivo não encontrado", ajuste o caminho no script.

---

## 7. Problemas comuns

| Sintoma | Causa provável | Solução |
| --- | --- | --- |
| API não sobe: `Could not resolve placeholder 'DB_PASSWORD'` ou `'JWT_SECRET'` | Variáveis não carregadas no terminal | Rode o `source .env` (ou o comando PowerShell) **no mesmo terminal** do `mvn` |
| Erro de chave JWT fraca / Base64 inválido | `JWT_SECRET` curto ou não-Base64 | Gere de novo com `openssl rand -base64 48` |
| `docker compose up` falha com "defina DB_PASSWORD no arquivo .env" | `.env` ausente em `backend/` | Crie o `.env` (seção 3.2) |
| `Connection refused` na porta 5432 | Postgres parado ou outro Postgres usando a porta | `docker compose ps`; pare o Postgres local ou mude a porta |
| `password authentication failed` após trocar a senha | O volume já foi criado com a senha antiga | `docker compose down -v` e suba de novo (**apaga os dados**) |
| App mostra "Não foi possível conectar ao servidor" | URL errada para o ambiente | Confira a tabela da seção 4.2; no celular use o IP do PC e `npx expo start -c` |
| Funciona no emulador mas não no celular | Firewall bloqueando a porta 8080 | Libere a porta 8080 no firewall do Windows/macOS |
| Erro de CORS ao rodar no navegador | Origem do Expo Web não liberada | Adicione a URL em `CORS_ALLOWED_ORIGINS` (ex.: `http://localhost:8081`) e reinicie a API |
| Expo reclama de `./assets/icon.png` | O `app.json` referencia um ícone que não está no repositório | Adicione um `assets/icon.png` ou remova a linha `"icon"` do `app.json` |
| Sessão cai de repente | Token expirou (padrão 7 dias) ou `JWT_SECRET` mudou | Faça login novamente |
