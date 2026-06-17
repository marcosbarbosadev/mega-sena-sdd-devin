import { Injectable, signal, computed, Injector, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ContaResponse } from '../models';
import { MockDataService } from '../mock/mock-data.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _user = signal<ContaResponse | null>(null);
  private readonly _loading = signal(true);
  private readonly _token = signal<string | null>(null);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly isAuthenticated = computed(() => !!this._token());
  readonly isAdmin = computed(() => this._user()?.papel === 'ADMINISTRADOR');
  readonly accountState = computed(() => this._user()?.estado ?? null);

  private firebaseApp: import('firebase/app').FirebaseApp | null = null;
  private firebaseAuth: import('firebase/auth').Auth | null = null;

  private mockData = environment.useMockAuth ? inject(MockDataService) : null;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    if (environment.useMockAuth) {
      this.initMock();
    } else {
      this.initFirebase();
    }
  }

  private initMock(): void {
    const savedEmail = localStorage.getItem('mock_user_email');
    if (savedEmail) {
      const profile = this.mockData!.getProfile(savedEmail);
      if (profile) {
        this.mockData!.setCurrentUser(savedEmail);
        this._token.set('mock-token-' + savedEmail);
        this._user.set(profile);
      }
    }
    this._loading.set(false);
  }

  private async initFirebase(): Promise<void> {
    const { initializeApp } = await import('firebase/app');
    const { getAuth, onAuthStateChanged } = await import('firebase/auth');

    this.firebaseApp = initializeApp(environment.firebase);
    this.firebaseAuth = getAuth(this.firebaseApp);

    onAuthStateChanged(this.firebaseAuth, async (firebaseUser) => {
      if (firebaseUser) {
        const token = await firebaseUser.getIdToken();
        this._token.set(token);
        localStorage.setItem('auth_token', token);
        await this.loadProfile();
      } else {
        this.clearState();
      }
      this._loading.set(false);
    });
  }

  getToken(): string | null {
    return this._token() ?? localStorage.getItem('auth_token');
  }

  async loginWithEmail(email: string, password: string): Promise<void> {
    if (environment.useMockAuth) {
      return this.mockLogin(email, password);
    }
    const { signInWithEmailAndPassword } = await import('firebase/auth');
    const credential = await signInWithEmailAndPassword(this.firebaseAuth!, email, password);
    const token = await credential.user.getIdToken();
    this._token.set(token);
    localStorage.setItem('auth_token', token);
    await this.loadProfile();
  }

  async registerWithEmail(email: string, password: string): Promise<void> {
    if (environment.useMockAuth) {
      return this.mockRegister(email, password);
    }
    const { createUserWithEmailAndPassword } = await import('firebase/auth');
    const credential = await createUserWithEmailAndPassword(this.firebaseAuth!, email, password);
    const token = await credential.user.getIdToken();
    this._token.set(token);
    localStorage.setItem('auth_token', token);
    await this.loadProfile();
  }

  async loginWithGoogle(): Promise<void> {
    if (environment.useMockAuth) {
      throw { code: 'auth/operation-not-supported-in-this-environment' };
    }
    const { signInWithPopup, GoogleAuthProvider } = await import('firebase/auth');
    const credential = await signInWithPopup(this.firebaseAuth!, new GoogleAuthProvider());
    const token = await credential.user.getIdToken();
    this._token.set(token);
    localStorage.setItem('auth_token', token);
    await this.loadProfile();
  }

  async logout(): Promise<void> {
    if (environment.useMockAuth) {
      this.mockData!.setCurrentUser(null);
      localStorage.removeItem('mock_user_email');
      this.clearState();
      this.router.navigate(['/login']);
      return;
    }
    try {
      await this.http.post(`${environment.apiUrl}/auth/logout`, {}).toPromise();
    } catch {
      // Ignore backend errors during logout
    }
    const { signOut } = await import('firebase/auth');
    await signOut(this.firebaseAuth!);
    this.clearState();
    this.router.navigate(['/login']);
  }

  async refreshToken(): Promise<void> {
    if (environment.useMockAuth) return;
    const user = this.firebaseAuth?.currentUser;
    if (user) {
      const token = await user.getIdToken(true);
      this._token.set(token);
      localStorage.setItem('auth_token', token);
    }
  }

  private mockLogin(email: string, password: string): void {
    const profile = this.mockData!.login(email, password);
    if (!profile) {
      throw { code: 'auth/invalid-credential' };
    }
    this._token.set('mock-token-' + email);
    this._user.set(profile);
    this.mockData!.setCurrentUser(email);
    localStorage.setItem('mock_user_email', email);
  }

  private mockRegister(email: string, password: string): void {
    const profile = this.mockData!.register(email, password);
    if (!profile) {
      throw { code: 'auth/email-already-in-use' };
    }
    this._token.set('mock-token-' + email);
    this._user.set(profile);
    this.mockData!.setCurrentUser(email);
    localStorage.setItem('mock_user_email', email);
  }

  private async loadProfile(): Promise<void> {
    try {
      const profile = await this.http
        .get<ContaResponse>(`${environment.apiUrl}/auth/me`)
        .toPromise();
      if (profile) {
        this._user.set(profile);
      }
    } catch {
      // Profile load failed
    }
  }

  private clearState(): void {
    this._token.set(null);
    this._user.set(null);
    localStorage.removeItem('auth_token');
  }
}
