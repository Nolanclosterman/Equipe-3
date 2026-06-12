import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AssetManifest, Company, Profile } from '../models/profile.model';

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
  private readonly _generating = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _manifest = signal<AssetManifest>({ enabled: false, assets: {} });

  // --- public read-only views ---
  readonly profile = this._profile.asReadonly();
  readonly loading = this._loading.asReadonly();
  /** True while an AI call (event generation, scoring, chat) is in flight. */
  readonly generating = this._generating.asReadonly();
  readonly error = this._error.asReadonly();

  readonly company = computed<Company | null>(() => this._profile()?.company ?? null);
  readonly hasCompany = computed(() => this.company() !== null);
  readonly isActive = computed(() => this.company()?.active ?? false);
  /** True when the backend can generate images (OpenAI key configured). */
  readonly imagesEnabled = computed(() => this._manifest().enabled);

  constructor(private readonly http: HttpClient) {}

  /** URL of a one-shot asset ('avatar-1', 'ui-loader', …) or null if absent. */
  asset(key: string): string | null {
    return this._manifest().assets[key] ?? null;
  }

  /** GET /assets/manifest — which generated avatars / UI icons exist. */
  async loadManifest(): Promise<void> {
    try {
      this._manifest.set(
        await firstValueFrom(this.http.get<AssetManifest>(`${API_BASE}/assets/manifest`))
      );
    } catch {
      // No manifest: keep the emoji fallbacks, nothing to report to the player.
    }
  }

  /**
   * Silent profile refresh used to poll for the event images generated in the
   * background. Never touches the loading/error flags, and is skipped when a
   * regular action is in flight to avoid overwriting fresher local state.
   */
  async refreshQuietly(name: string): Promise<void> {
    if (this._loading() || this._generating()) return;
    try {
      const profile = await firstValueFrom(
        this.http.get<Profile>(`${API_BASE}/profiles/${encodeURIComponent(name)}`)
      );
      if (!this._loading() && !this._generating()) {
        this._profile.set(profile);
      }
    } catch {
      // Polling only: a failed refresh is harmless, the next tick retries.
    }
  }

  /** GET /profiles/{name} — load the connected profile and active company. */
  async loadProfile(name: string): Promise<void> {
    await this.run(() =>
      firstValueFrom(this.http.get<Profile>(`${API_BASE}/profiles/${encodeURIComponent(name)}`))
    );
  }

  /** POST /profiles/{name}/company — create a company when none exists. */
  async createCompany(
    name: string,
    companyName: string,
    type: string | null,
    character: string | null,
    avatar: string | null
  ): Promise<void> {
    await this.run(async () => {
      const company = await firstValueFrom(
        this.http.post<Company>(`${API_BASE}/profiles/${encodeURIComponent(name)}/company`, {
          name: companyName,
          type,
          character,
          avatar,
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

  /** POST /profiles/{name}/company/chat — send a player message (AI help). */
  async sendMessage(name: string, message: string): Promise<void> {
    await this.run(async () => {
      const company = await firstValueFrom(
        this.http.post<Company>(
          `${API_BASE}/profiles/${encodeURIComponent(name)}/company/chat`,
          { message }
        )
      );
      return this.withCompany(name, company);
    }, true);
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
    }, true);
  }

  /** POST /profiles/{name}/company/event — generate the next game event. */
  async generateEvent(name: string): Promise<void> {
    await this.run(async () => {
      const company = await firstValueFrom(
        this.http.post<Company>(
          `${API_BASE}/profiles/${encodeURIComponent(name)}/company/event`,
          {}
        )
      );
      return this.withCompany(name, company);
    }, true);
  }

  /** POST /profiles/{name}/company/decision — submit the chosen solution. */
  async decide(name: string, solution: string): Promise<void> {
    await this.run(async () => {
      const company = await firstValueFrom(
        this.http.post<Company>(
          `${API_BASE}/profiles/${encodeURIComponent(name)}/company/decision`,
          { solution }
        )
      );
      return this.withCompany(name, company);
    }, true);
  }

  private withCompany(name: string, company: Company): Profile {
    return { name: this._profile()?.name ?? name, company };
  }

  /**
   * Runs an action, managing loading/error flags and committing the profile.
   * When {@code generating} is true the dedicated AI-busy flag is raised too,
   * so the UI can show the funny loading animation.
   */
  private async run(
    action: () => Promise<Profile | void>,
    generating = false
  ): Promise<void> {
    this._loading.set(true);
    if (generating) this._generating.set(true);
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
      if (generating) this._generating.set(false);
    }
  }

  private toMessage(err: unknown): string {
    if (typeof err === 'object' && err !== null && 'status' in err) {
      const status = (err as { status: number }).status;
      // Surface the backend's kid-friendly message (e.g. name moderation).
      const body = (err as { error?: { message?: string } }).error;
      if (status === 400 && body?.message) return body.message;
      if (status === 0) return "Impossible de joindre le serveur. Le backend est-il démarré ?";
      if (status === 409) return 'Action impossible pour le moment.';
      if (status === 404) return 'Aucune entreprise trouvée.';
      return `Erreur serveur (${status}).`;
    }
    return 'Une erreur inattendue est survenue.';
  }
}
