import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from '../../services/profile.service';

/**
 * Frame 1 — Entreprise.
 * CAS 1: no company yet -> name input + "Créer".
 * CAS 2: existing company -> "Démarrer" / "Supprimer".
 */
@Component({
  selector: 'app-company',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  templateUrl: './company.html',
  styleUrl: './company.scss',
})
export class CompanyView {
  /** Connected player name. */
  readonly playerName = input.required<string>();

  protected readonly store = inject(ProfileService);
  protected readonly newName = signal('');

  protected async create(): Promise<void> {
    const name = this.newName().trim();
    if (!name) return;
    await this.store.createCompany(this.playerName(), name);
    this.newName.set('');
  }

  protected start(): Promise<void> {
    return this.store.startCompany(this.playerName());
  }

  protected delete(): Promise<void> {
    return this.store.deleteCompany(this.playerName());
  }
}
