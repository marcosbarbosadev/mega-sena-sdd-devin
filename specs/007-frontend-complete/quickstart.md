# Quickstart - Frontend Completo

Guia rápido para configurar e executar o frontend do Mega Sena Manager em Angular.

## Pré-requisitos

- Node.js 18+ (LTS)
- npm 9+ ou yarn 1.22+
- Angular CLI 17+ (instalado globalmente ou via npx)
- Backend rodando (ver quickstart do backend)
- Projeto Firebase configurado com Authentication

## Configuração do Firebase

1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/)
2. Ative **Authentication** com:
   - **Email/Password**
   - **Google** sign-in
3. Vá em **Project Settings** > **General** > **Your apps**
4. Adicione app **Web** e copie a configuração (firebaseConfig)

## Criação do Projeto Angular

### Via Angular CLI

```bash
# Criar novo projeto
ng new megasena-frontend \
  --style=scss \
  --routing=true \
  --ssr=false \
  --skip-git

cd megasena-frontend
```

### Instalar Dependências

```bash
# Angular Material (opcional, para componentes UI)
ng add @angular/material

# Firebase
npm install firebase

# HTTP client (já incluído no Angular)
# No需要额外安装

# Ferramentas de desenvolvimento
npm install -D @types/node
```

## Configuração do Projeto

### 1. Configurar Firebase

No arquivo `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  firebase: {
    apiKey: 'sua-api-key',
    authDomain: 'seu-projeto.firebaseapp.com',
    projectId: 'seu-projeto-id',
    storageBucket: 'seu-projeto.appspot.com',
    messagingSenderId: 'seu-sender-id',
    appId: 'seu-app-id'
  }
};
```

No arquivo `src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.megasena.com/api',  // URL de produção
  firebase: {
    // Configuração de produção do Firebase
  }
};
```

### 2. Configurar HttpClientModule

No `app.config.ts` (Angular 17+) ou `app.module.ts` (versões anteriores):

```typescript
import { provideHttpClient, withInterceptors } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptors([AuthInterceptor])),
    // ... outros providers
  ]
};
```

### 3. Habilitar Google Sign-In

No Firebase Console, habilite Google Sign-In e configure o OAuth consent screen.

## Estrutura Inicial

```bash
# Criar estrutura de módulos
ng g module core --module app
ng g module features/auth --module app
ng g module features/admin --module app
ng g module features/games --module app
ng g module features/conference --module app
ng g module shared --module app
```

## Executar em Desenvolvimento

```bash
# Instalar dependências
npm install

# Executar servidor de desenvolvimento
ng serve

# Acesse http://localhost:4200
```

## Build para Produção

```bash
# Build otimizado
ng build --configuration production

# Output em dist/megasena-frontend/
```

## Deploy

### Opções de Deploy

#### 1. Static Hosting (Vercel/Netlify)

```bash
# Vercel
npm install -g vercel
vercel

# Netlify
npm install -g netlify-cli
netlify deploy --prod
```

#### 2. GitHub Pages

```bash
# Instalar angular-cli-ghpages
ng add angular-cli-ghpages

# Deploy
ng deploy --base-href=/repo-name/
```

#### 3. Docker (opcional)

```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/megasena-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## Testes

### Unitários

```bash
# Executar testes unitários
ng test

# Com cobertura
ng test --coverage
```

### E2E

```bash
# Executar testes E2E
ng e2e
```

## Configuração do Backend CORS

Certifique-se de que o backend está configurado para aceitar requisições do frontend. No Spring Boot:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")  // URL do frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

## Exemplos de Uso

### 1. Integrar Firebase Authentication

```typescript
// auth.service.ts
import { Injectable } from '@angular/core';
import { initializeApp } from 'firebase/app';
import { 
  getAuth, 
  signInWithEmailAndPassword, 
  signInWithPopup, 
  GoogleAuthProvider,
  signOut 
} from 'firebase/auth';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private auth = getAuth(initializeApp(environment.firebase));
  private googleProvider = new GoogleAuthProvider();

  async loginEmail(email: string, password: string) {
    return await signInWithEmailAndPassword(this.auth, email, password);
  }

  async loginGoogle() {
    return await signInWithPopup(this.auth, this.googleProvider);
  }

  async logout() {
    return await signOut(this.auth);
  }

  getToken(): string | null {
    return this.auth.currentUser?.getIdToken() || null;
  }
}
```

### 2. Fazer Requisição Autenticada

```typescript
// api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getGames() {
    return this.http.get<Game[]>(`${this.baseUrl}/jogos`);
  }

  createGame(request: GameRequest) {
    return this.http.post<Game>(`${this.baseUrl}/jogos`, request);
  }
}
```

### 3. Implementar Auth Interceptor

```typescript
// auth.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('auth_token');
    if (token) {
      request = request.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    return next.handle(request);
  }
}
```

## Troubleshooting

### Erro de CORS

**Sintoma**: Requisições falham com erro de CORS no console do browser.

**Solução**:
1. Verifique configuração CORS no backend
2. Certifique-se de que a URL do frontend está na whitelist
3. Em desenvolvimento, use proxy do Angular CLI

```bash
# Usar proxy
ng serve --proxy-config proxy.conf.json
```

`proxy.conf.json`:
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false
  }
}
```

### Firebase Authentication Falha

**Sintoma**: Login Firebase não funciona ou retorna erro.

**Solução**:
1. Verifique configuração do Firebase no environment.ts
2. Certifique-se de que Authentication está habilitado no console
3. Verifique se Google Sign-In está configurado corretamente
4. Verifique se domínio está autorizado no Firebase Console

### Build Falha

**Sintoma**: `ng build --prod` falha com erros.

**Solução**:
1. Verifique se todas as dependências estão instaladas
2. Limpe cache: `rm -rf node_modules package-lock.json && npm install`
3. Verifique se não há erros de TypeScript: `ng build`
4. Verifique se ambiente de produção está configurado

### Rotas Não Funcionam

**Sintoma**: Navegação entre páginas não funciona.

**Solução**:
1. Verifique se RouterModule está configurado corretamente
2. Verifique se guards estão implementados corretamente
3. Para deploy em GitHub Pages, use `--base-href`

## Próximos Passos

- [ ] Implementar fluxo de login completo
- [ ] Criar layout base (header, navegação)
- [ ] Implementar dashboard do usuário
- [ ] Implementar cadastro de jogos
- [ ] Implementar conferência
- [ ] Implementar módulo admin
- [ ] Adicionar testes
- [ ] Configurar deploy em produção
- [ ] Configurar monitoramento de erros (Sentry, etc.)
- [ ] Adicionar analytics (Google Analytics, etc.)