import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from '../../services/profile.service';
import { AVATARS, TYPES, TypeChoice, avatarByLabel } from '../../data/assets-catalog';

/**
 * INITIALIZATION screen, following mockups/img.png ("CRÉE TA ENTREPRISE !").
 * CAS 1: no company yet -> pick a profile picture, an activity (predefined
 *        icon grid OR a custom activity the player writes) and a name.
 * CAS 2: company created -> generate the icon, then start the adventure
 *        (which also generates the first event).
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
  protected readonly avatar = signal(AVATARS[0]);
  /** Selected predefined activity, or 'custom' when writing one's own. */
  protected readonly typeChoice = signal<string | null>(null);
  protected readonly customType = signal('');

  protected readonly avatars = AVATARS;
  protected readonly types = TYPES;

  /** The activity sent to the backend: predefined label or the custom text. */
  protected readonly companyType = computed(() =>
    this.typeChoice() === 'custom' ? this.customType().trim() || null : this.typeChoice()
  );

  protected readonly canCreate = computed(
    () =>
      this.newName().trim().length > 0 &&
      this.typeChoice() !== null &&
      (this.typeChoice() !== 'custom' || this.customType().trim().length > 0)
  );

  /** Avatar of the created company's owner, for the CAS 2 recap. */
  protected readonly companyAvatar = computed(() =>
    avatarByLabel(this.store.company()?.character ?? null)
  );

  protected pickType(t: TypeChoice): void {
    this.typeChoice.set(t.label);
    if (!this.newName().trim()) {
      this.newName.set(t.sample);
    }
  }

  protected async create(): Promise<void> {
    if (!this.canCreate()) return;
    await this.store.createCompany(
      this.playerName(),
      this.newName().trim(),
      this.companyType(),
      this.avatar().label
    );
  }

  protected generateIcon(): Promise<void> {
    return this.store.generateIcon(this.playerName());
  }

  protected async start(): Promise<void> {
    await this.store.startCompany(this.playerName());
    // Kick off the first event so the game loop starts immediately.
    await this.store.generateEvent(this.playerName());
  }

  protected delete(): Promise<void> {
    return this.store.deleteCompany(this.playerName());
  }
}
