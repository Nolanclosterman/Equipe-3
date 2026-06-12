import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from '../../services/profile.service';

interface CompanyType {
  emoji: string;
  label: string;
  sample: string;
}

/**
 * INITIALIZATION screen.
 * CAS 1: no company yet -> pick an avatar, a company type and a name.
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
  protected readonly avatar = signal('👾');
  protected readonly type = signal<string | null>(null);

  protected readonly avatars = ['👾', '🧑‍🚀', '🤖', '🦖', '🦸', '🧙', '🐉', '🦄'];

  protected readonly types: CompanyType[] = [
    { emoji: '🍔', label: 'Resto rigolo', sample: 'Burger Galaxie' },
    { emoji: '🎮', label: 'Jeux vidéo', sample: 'Pixel Power' },
    { emoji: '👕', label: 'Mode', sample: 'Super Style' },
    { emoji: '🤖', label: 'Robots', sample: 'RoboCopains' },
    { emoji: '🌱', label: 'Éco & nature', sample: 'Planète Verte' },
    { emoji: '🚀', label: 'Techno', sample: 'Fusée Tech' },
  ];

  protected pickType(t: CompanyType): void {
    this.type.set(t.label);
    if (!this.newName().trim()) {
      this.newName.set(t.sample);
    }
  }

  protected async create(): Promise<void> {
    const name = this.newName().trim();
    if (!name) return;
    await this.store.createCompany(this.playerName(), name, this.type(), this.avatar());
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
