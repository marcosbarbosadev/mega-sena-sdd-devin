# Mega Sena Manager — Frontend

Frontend Angular 19 para o sistema de gerenciamento de jogos da Mega Sena.

## Pré-requisitos

- Node.js 18+ (LTS)
- npm 9+

## Instalação

```bash
cd frontend
npm install
```

## Modo Mock (Desenvolvimento Local sem Firebase)

O modo mock permite acessar o sistema localmente **sem backend e sem provedor de identidade (Firebase)**. Todas as APIs são simuladas no navegador com dados em memória.

### Como ativar

O modo mock já vem **ativado por padrão** no ambiente de desenvolvimento (`environment.ts`):

```typescript
// src/environments/environment.ts
export const environment = {
  useMockAuth: true,  // ← modo mock ativado
  ...
};
```

Para desativar (e usar Firebase real + backend), altere para `false`.

### Passo a passo

1. **Instale as dependências e inicie o servidor:**

   ```bash
   cd frontend
   npm install
   npx ng serve
   ```

2. **Acesse no navegador:** `http://localhost:4200`

3. **Login como Administrador:**
   - E-mail: `admin@local.com`
   - Senha: `123456`
   - Você será redirecionado para o painel administrativo

4. **Cadastrar um novo usuário (ficará pendente):**
   - Na tela de login, clique na aba **"Cadastrar"**
   - Preencha com qualquer e-mail (ex: `usuario@teste.com`) e uma senha qualquer
   - O usuário será criado com estado **PENDENTE** e você verá a tela de "Conta Pendente"

5. **Aprovar o usuário pelo admin:**
   - Faça logout do usuário pendente (botão "Sair" na tela de pendente)
   - Faça login como admin: `admin@local.com` / `123456`
   - Vá em **"Moderação"** no menu
   - Você verá o usuário pendente na lista
   - Clique em **"Aprovar"** (ou **"Reprovar"** informando um motivo)

6. **Acessar como o usuário aprovado:**
   - Faça logout do admin
   - Faça login com o e-mail/senha do usuário que você cadastrou
   - Agora ele terá acesso ao dashboard, jogos, conferência e concursos

### Funcionalidades disponíveis no modo mock

| Funcionalidade | Descrição |
|---|---|
| Login/Cadastro | E-mail e senha (sem Google no mock) |
| Dashboard | Estatísticas de jogos cadastrados |
| Cadastrar Jogo | Manual (selecionar dezenas) ou Automático |
| Listar Jogos | Ver, editar e excluir jogos |
| Conferência | Resultados dos jogos contra concurso mockado (2750) |
| Concursos | Visualizar concurso mais recente e buscar por número |
| Admin - Moderação | Aprovar/reprovar contas pendentes |
| Admin - Sincronização | Ver status e disparar sync (simulado) |

### Dados mockados

- **Concurso 2750:** dezenas [5, 12, 23, 34, 45, 56], prêmio R$ 35.000.000
- **Concurso 2749:** dezenas [3, 17, 29, 38, 44, 52], prêmio R$ 22.000.000
- Jogos criados ficam apenas em memória (são perdidos ao recarregar a página)
- Sessão do usuário logado persiste via `localStorage`

## Servidor de Desenvolvimento (com Backend)

Para usar com o backend real (Spring Boot + Firebase):

1. Configure o Firebase em `src/environments/environment.ts`:
   ```typescript
   export const environment = {
     useMockAuth: false,  // ← desativar mock
     apiUrl: 'http://localhost:8080/api',
     firebase: {
       apiKey: 'SUA_API_KEY',
       authDomain: 'SEU_PROJETO.firebaseapp.com',
       projectId: 'SEU_PROJECT_ID',
       ...
     }
   };
   ```

2. Inicie o backend na porta 8080

3. Execute:
   ```bash
   npx ng serve
   ```

4. Acesse `http://localhost:4200`

## Build

```bash
npx ng build
```

Os artefatos ficam em `dist/megasena-frontend/`.

## Testes

```bash
npx ng test
```

## Estrutura do Projeto

```
src/app/
├── core/
│   ├── guards/          # authGuard, adminGuard, unauthGuard
│   ├── interceptors/    # authInterceptor (JWT)
│   ├── mock/            # MockDataService, mockApiInterceptor
│   ├── models/          # Interfaces (DTOs)
│   └── services/        # AuthService, ApiService, NotificationService
├── features/
│   ├── admin/           # Dashboard, moderação, sincronização
│   ├── auth/            # Login, perfil, estado pendente/reprovado
│   ├── concursos/       # Último concurso, busca
│   ├── conference/      # Conferência de jogos
│   └── games/           # Dashboard, lista, criar, editar
├── layout/              # Header, footer
└── shared/              # Loading, error-message, notifications
```
