import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { initializeApp, FirebaseApp } from 'firebase/app';
import {
  getAuth,
  signInWithEmailAndPassword,
  signInWithPopup,
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  signOut,
  onAuthStateChanged,
  Auth,
  User
} from 'firebase/auth';
import { environment } from '../../../environments/environment';
import { ContaResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private app: FirebaseApp;
  private auth: Auth;
  private googleProvider = new GoogleAuthProvider();

  private readonly _user = signal<ContaResponse | null>(null);
  private readonly _loading = signal(true);
  private readonly _token = signal<string | null>(null);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly isAuthenticated = computed(() => !!this._token());
  readonly isAdmin = computed(() => this._user()?.papel === 'ADMINISTRADOR');
  readonly accountState = computed(() => this._user()?.estado ?? null);

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.app = initializeApp(environment.firebase);
    this.auth = getAuth(this.app);

    onAuthStateChanged(this.auth, async (firebaseUser) => {
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
    const credential = await signInWithEmailAndPassword(this.auth, email, password);
    const token = await credential.user.getIdToken();
    this._token.set(token);
    localStorage.setItem('auth_token', token);
    await this.loadProfile();
  }

  async registerWithEmail(email: string, password: string): Promise<void> {
    const credential = await createUserWithEmailAndPassword(this.auth, email, password);
    const token = await credential.user.getIdToken();
    this._token.set(token);
    localStorage.setItem('auth_token', token);
    await this.loadProfile();
  }

  async loginWithGoogle(): Promise<void> {
    const credential = await signInWithPopup(this.auth, this.googleProvider);
    const token = await credential.user.getIdToken();
    this._token.set(token);
    localStorage.setItem('auth_token', token);
    await this.loadProfile();
  }

  async logout(): Promise<void> {
    try {
      await this.http.post(`${environment.apiUrl}/auth/logout`, {}).toPromise();
    } catch {
      // Ignore backend errors during logout
    }
    await signOut(this.auth);
    this.clearState();
    this.router.navigate(['/login']);
  }

  async refreshToken(): Promise<void> {
    const user = this.auth.currentUser;
    if (user) {
      const token = await user.getIdToken(true);
      this._token.set(token);
      localStorage.setItem('auth_token', token);
    }
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
      // Profile load failed - user may not exist in backend yet
    }
  }

  private clearState(): void {
    this._token.set(null);
    this._user.set(null);
    localStorage.removeItem('auth_token');
  }
}
