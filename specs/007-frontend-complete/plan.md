# Plano de Implementação - Frontend Completo

## Visão Geral

Este plano descreve a implementação do frontend completo do Mega Sena Manager em Angular LTS, consumindo todas as APIs do backend (features 001-005).

## Stack Tecnológica

- **Framework**: Angular LTS (versão estável mais recente)
- **Linguagem**: TypeScript
- **UI Library**: Angular Material (ou similar)
- **State Management**: Services + Signals (Angular 16+)
- **HTTP Client**: HttpClient do Angular
- **Routing**: Angular Router com lazy loading
- **Forms**: Reactive Forms
- **Validation**: Validators nativos + custom validators
- **Testing**: Jest/Karma (unitários) + Cypress/Playwright (E2E)
- **Build**: Angular CLI

## Arquitetura

### Estrutura de Módulos

```
src/app/
├── core/                    # Módulo principal singleton
│   ├── services/
│   │   ├── auth.service.ts
│   │   ├── api.service.ts
│   │   └── storage.service.ts
│   ├── guards/
│   │   ├── auth.guard.ts
│   │   └── admin.guard.ts
│   ├── interceptors/
│   │   └── auth.interceptor.ts
│   └── models/              # Interfaces e tipos
├── features/
│   ├── auth/
│   │   ├── login/
│   │   ├── profile/
│   │   └── pending-state/
│   ├── admin/
│   │   ├── dashboard/
│   │   ├── moderation/
│   │   └── sync/
│   ├── games/
│   │   ├── dashboard/
│   │   ├── list/
│   │   ├── create/
│   │   └── edit/
│   ├── conference/
│   │   └── list/
│   └── concursos/
│       └── latest/
├── shared/
│   ├── components/          # Componentes reutilizáveis
│   ├── directives/
│   └── pipes/
└── layout/
    ├── header/
    ├── sidebar/
    └── footer/
```

### Módulos Funcionais

#### Auth Module
- LoginComponent: Tela de login com Firebase
- ProfileComponent: Perfil do usuário
- PendingStateComponent: Tela para conta pendente/reprovada
- AuthService: Integração com Firebase e backend

#### Admin Module
- AdminDashboardComponent: Dashboard administrativo
- ModerationListComponent: Lista de contas pendentes
- ModerationActionComponent: Aprovar/reprovar contas
- SyncStatusComponent: Status e controle de sincronização
- AdminService: Serviços específicos de admin

#### Games Module
- UserDashboardComponent: Dashboard do usuário
- GamesListComponent: Lista de jogos do usuário
- GameCreateComponent: Cadastro de jogos (manual/gerado)
- GameEditComponent: Edição de jogos
- GameService: CRUD de jogos

#### Conference Module
- ConferenceListComponent: Lista de conferências
- ConferenceItemComponent: Item individual de conferência
- ConferenceService: Consulta de conferências

#### Concursos Module
- LatestConcursoComponent: Mostra último concurso
- ConcursoService: Dados de concursos

## Fluxos de UI

### 1. Fluxo de Login

```
Usuario não autenticado
    ↓
Acessa aplicação
    ↓
AuthGuard redireciona para /login
    ↓
Tela de Login (Firebase Auth)
    ↓
Usuário escolhe: Email+Senha ou Google
    ↓
Firebase retorna token JWT
    ↓
AuthService chama /api/contas (cria/atualiza conta)
    ↓
AuthService verifica estado da conta
    ↓
Se ATIVO → Redireciona para /dashboard
Se PENDENTE → Redireciona para /pending
Se REPROVADO → Redireciona para /rejected
```

### 2. Fluxo de Cadastro de Jogos

```
Usuário autenticado (ativo)
    ↓
Acessa /games/create
    ↓
GameCreateComponent: escolha de modo
    ↓
Usuário escolhe: Manual ou Gerado
    ↓
[Manual] Usuário seleciona 6-9 dezenas
[Gerado] Usuário escolhe quantidade (6-9)
    ↓
Validação local (imediata)
    ↓
Usuário clica "Salvar"
    ↓
GameService POST /api/jogos
    ↓
Backend retorna jogo criado com concurso vinculado
    ↓
Mostra confirmação com detalhes
    ↓
Opção: "Cadastrar outro" ou "Ver meus jogos"
```

### 3. Fluxo de Moderação (Admin)

```
Admin autenticado
    ↓
Acessa /admin/moderation
    ↓
ModerationListComponent carrega /api/admin/moderacao/pendentes
    ↓
Mostra lista de contas pendentes
    ↓
Admin clica em "Aprovar" em uma conta
    ↓
ModerationActionComponent confirma
    ↓
AdminService POST /api/admin/moderacao/{id}/aprovar
    ↓
Backend aprova conta
    ↓
Lista atualiza sem recarregar (remove item aprovado)
    ↓
[Alternativa] Admin clica em "Reprovar"
    ↓
Modal pede motivo obrigatório
    ↓
AdminService POST /api/admin/moderacao/{id}/reprovar
    ↓
Backend reprova conta
    ↓
Lista atualiza (remove item reprovado)
```

### 4. Fluxo de Conferência

```
Usuário autenticado (ativo)
    ↓
Acessa /conference
    ↓
ConferenceListComponent carrega /api/conferencia
    ↓
Mostra lista de jogos com resultado
    ↓
Jogos sorteados: mostram acertos, faixa, premiado
Jogos não sorteados: mostram "Aguardando sorteio"
    ↓
Usuário pode clicar em jogo para detalhes
    ↓
ConferenceItemComponent mostra detalhes completos
    ↓
[Opcional] Polling automático para atualizações
```

## Integração com Backend

### API Client (ApiService)

Service centralizado para comunicação com backend:

```typescript
@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Auth
  createAccount(token: string) { }
  getProfile() { }
  logout() { }

  // Admin
  getPendingAccounts() { }
  approveAccount(id: string) { }
  rejectAccount(id: string, motivo: string) { }
  getSyncStatus() { }
  triggerSync() { }

  // Games
  getGames() { }
  createGame(request: GameRequest) { }
  updateGame(id: string, request: GameRequest) { }
  deleteGame(id: string) { }

  // Conference
  getConferences() { }
  getConference(gameId: string) { }

  // Concursos
  getLatestConcurso() { }
  getConcurso(numero: number) { }
}
```

### Auth Interceptor

Interceptor para adicionar token JWT em todas as requisições:

```typescript
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();
    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    return next.handle(req);
  }
}
```

### Error Handling

Handler global para erros de API:

```typescript
@Injectable()
export class ErrorHandler implements HttpErrorHandler {
  handle(error: HttpErrorResponse): Observable<any> {
    if (error.status === 401) {
      this.authService.logout();
      this.router.navigate(['/login']);
    }
    // Tratamento de outros erros
    return throwError(() => error);
  }
}
```

## Componentes Principais

### LoginComponent

**Responsabilidades**:
- Integração com Firebase Authentication
- Opções: Email+Senha e Google
- Tratamento de erros de autenticação
- Redirecionamento após login

**Inputs**: Nenhum
**Outputs**: Nenhum (emite evento via AuthService)

### GameCreateComponent

**Responsabilidades**:
- Interface para cadastro de jogos
- Modo manual: seleção de dezenas
- Modo gerado: escolha de quantidade
- Validação local e backend
- Mostra concurso vinculado após salvar

**Inputs**: Nenhum
**Outputs**: `gameCreated` (emite quando jogo criado)

### ModerationListComponent

**Responsabilidades**:
- Lista contas pendentes
- Ações de aprovar/reprovar
- Modal de motivo para reprovação
- Atualização reativa da lista

**Inputs**: Nenhum
**Outputs**: Nenhum

### ConferenceListComponent

**Responsabilidades**:
- Lista jogos com conferência
- Indicador visual de premiação
- Destaque para sena/quina/quadra
- Status "aguardando sorteio"

**Inputs**: Nenhum
**Outputs**: Nenhum

## State Management

### Services como State

Usar Angular Services com Signals (Angular 16+) para estado reactivo:

```typescript
@Injectable({ providedIn: 'root' })
export class GameState {
  private games = signal<Game[]>([]);
  private loading = signal(false);
  private error = signal<string | null>(null);

  games = computed(() => this.games());
  isLoading = computed(() => this.loading());
  errorMessage = computed(() => this.error());

  async loadGames() {
    this.loading.set(true);
    try {
      const games = await this.apiService.getGames();
      this.games.set(games);
    } catch (error) {
      this.error.set('Erro ao carregar jogos');
    } finally {
      this.loading.set(false);
    }
  }
}
```

### Local Storage

Persistir token e preferências:

```typescript
@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly TOKEN_KEY = 'auth_token';

  setToken(token: string) {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  clearToken() {
    localStorage.removeItem(this.TOKEN_KEY);
  }
}
```

## Routing

### Configuração de Rotas

```typescript
const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [UnauthGuard]
  },
  {
    path: 'dashboard',
    component: UserDashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard, AdminGuard]
  },
  {
    path: 'games',
    loadChildren: () => import('./features/games/games.module').then(m => m.GamesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'conference',
    loadChildren: () => import('./features/conference/conference.module').then(m => m.ConferenceModule),
    canActivate: [AuthGuard]
  }
];
```

### Guards

- **AuthGuard**: Verifica se usuário está autenticado
- **AdminGuard**: Verifica se usuário tem papel ADMIN
- **UnauthGuard**: Redireciona usuários autenticados para dashboard

## UI/UX Guidelines

### Design System

- **Cores**: Paleta limpa com destaque para premiações (ouro para sena, prata para quina, bronze para quadra)
- **Tipografia**: Fonte legível, hierarquia clara
- **Espaçamento**: Consistente, seguindo 8px grid system
- **Componentes**: Angular Material ou componentes customizados seguindo estilo consistente

### Responsividade

- **Mobile-first**: Design para mobile primeiro, progressive enhancement
- **Breakpoints**: 
  - Mobile: < 768px
  - Tablet: 768px - 1024px
  - Desktop: > 1024px
- **Layout**: Flexbox e CSS Grid para layouts responsivos

### Acessibilidade

- **Contraste**: Mínimo 4.5:1 para texto normal
- **Keyboard navigation**: Todas as ações acessíveis via teclado
- **ARIA labels**: Labels em componentes interativos
- **Focus indicators**: Visíveis em todos os elementos interativos

## Performance

### Otimizações

- **Lazy loading**: Módulos carregados sob demanda
- **Tree shaking**: Código não utilizado removido no build
- **Imagens**: Otimizadas e lazy carregadas
- **Bundle splitting**: Divisão inteligente de bundles
- **Cache**: Estratégias de cache para assets

### Métricas

- **FCP**: First Contentful Paint < 1.5s
- **LCP**: Largest Contentful Paint < 2.5s
- **TTI**: Time to Interactive < 3.5s
- **CLS**: Cumulative Layout Shift < 0.1
- **Lighthouse Score**: > 90

## Testes

### Estratégia de Testes

- **Unitários**: Componentes, Services, Guards, Interceptors
- **Integração**: Integração com serviços e API mockada
- **E2E**: Fluxos principais (login, cadastro, conferência, moderação)

### Ferramentas

- **Unitários**: Jest ou Karma + Jasmine
- **E2E**: Cypress ou Playwright
- **Mocking**: HttpClientTestingModule para mocks de API

### Cobertura

- **Target**: > 90% de cobertura de código
- **Critical paths**: 100% de cobertura em fluxos críticos

## Build e Deploy

### Build

```bash
# Desenvolvimento
ng serve

# Produção
ng build --configuration production
```

### Deploy

- **Static hosting**: Vercel, Netlify, GitHub Pages, ou CDN
- **Docker**: Opcional, containerizar app estático
- **CI/CD**: GitHub Actions ou similar para build automático

### Configuração de Ambiente

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  firebaseConfig: { ... }
};

// environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.megasena.com/api',
  firebaseConfig: { ... }
};
```

## Próximos Passos

- [ ] Configurar projeto Angular com CLI
- [ ] Configurar Firebase Authentication
- [ ] Implementar AuthInterceptor e Guards
- [ ] Criar layout base (header, navegação)
- [ ] Implementar fluxo de login
- [ ] Implementar dashboard do usuário
- [ ] Implementar cadastro de jogos
- [ ] Implementar conferência
- [ ] Implementar módulo admin
- [ ] Adicionar testes unitários
- [ ] Adicionar testes E2E
- [ ] Otimizar performance
- [ ] Deploy em produção