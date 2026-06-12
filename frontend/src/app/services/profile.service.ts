import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Company, Profile } from '../models/profile.model';

const API_BASE = '/api';

/**
 * Signal-based store for the connected profile and its company.
 * Components read the public computed signals and call the async actions;
 * all server state lives here in a single source of truth.
 */
@Injectable({ providedIn: 'root' })
export class ProfileService {
  // --- writable state ---
  private readonly _profile = signal<Profile | null>(null);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  // --- public read-only views ---
  readonly profile = this._profile.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly company = computed<Company | null>(() => this._profile()?.company ?? null);
  readonly hasCompany = computed(() => this.company() !== null);
  readonly isActive = computed(() => this.company()?.active ?? false);

  constructor(private readonly http: HttpClient) {}

  /** GET /profiles/{name} — load the connected profile and active company. */
  async loadProfile(name: string): Promise<void> {
    await this.run(() =>
      firstValueFrom(this.http.get<Profile>(`${API_BASE}/profiles/${encodeURIComponent(name)}`))
    );
  }

  /** POST /profiles/{name}/company — create a company when none exists. */
  async createCompany(name: string, companyName: string): Promise<void> {
    await this.run(async () => {
      const company = await firstValueFrom(
        this.http.post<Company>(`${API_BASE}/profiles/${encodeURIComponent(name)}/company`, {
          name: companyName,
        })
      );
      return this.withCompany(name, company);
    });
  }

  /** POST /profiles/{name}/company/start — activate the existing company. */
  async startCompany(name: string): Promise<void> {
    await this.run(async () => {
      const company = await firstValueFrom(
        this.http.post<Company>(
          `${API_BASE}/profiles/${encodeURIComponent(name)}/company/start`,
          {}
        )
      );
      return this.withCompany(name, company);
    });
  }

  /** DELETE /profiles/{name}/company — remove the existing company. */
  async deleteCompany(name: string): Promise<void> {
    await this.run(async () => {
      await firstValueFrom(
        this.http.delete<void>(`${API_BASE}/profiles/${encodeURIComponent(name)}/company`)
      );
      return { name, company: null } satisfies Profile;
    });
  }

  /** POST /profiles/{name}/company/chat — send a player message. */
  async sendMessage(name: string, message: string): Promise<void> {
    await this.run(async () => {
      const company = await firstValueFrom(
        this.http.post<Company>(
          `${API_BASE}/profiles/${encodeURIComponent(name)}/company/chat`,
          { message }
        )
      );
      return this.withCompany(name, company);
    });
  }

  /** POST /profiles/{name}/company/icon — (re)generate the company icon. */
  async generateIcon(name: string): Promise<void> {
    await this.run(async () => {
      const company = await firstValueFrom(
        this.http.post<Company>(
          `${API_BASE}/profiles/${encodeURIComponent(name)}/company/icon`,
          {}
        )
      );
      return this.withCompany(name, company);
    });
  }

  private withCompany(name: string, company: Company): Profile {
    return { name: this._profile()?.name ?? name, company };
  }

  /** Runs an action, managing loading/error flags and committing the profile. */
  private async run(action: () => Promise<Profile | void>): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const profile = await action();
      if (profile) {
        this._profile.set(profile);
      }
    } catch (err: unknown) {
      this._error.set(this.toMessage(err));
    } finally {
      this._loading.set(false);
    }
  }

  private toMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'status' in err) {
      const status = (err as { status: number }).status;
      if (status === 0) return "Impossible de joindre le serveur. Le backend est-il démarré ?";
      if (status === 409) return 'Une entreprise existe déjà pour ce joueur.';
      if (status === 404) return 'Aucune entreprise trouvée.';
      return `Erreur serveur (${status}).`;
    }
    return 'Une erreur inattendue est survenue.';
  }
}
