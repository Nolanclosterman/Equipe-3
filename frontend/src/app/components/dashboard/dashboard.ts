import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from '../../services/profile.service';
import { ScoreGauge } from '../score-gauge/score-gauge';

/**
 * Frame 2 — Dashboard (Accueil).
 * Shows the three indicators, the company image / icon generation, and the
 * assistant chat.
 */
@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ScoreGauge],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  readonly playerName = input.required<string>();

  protected readonly store = inject(ProfileService);
  protected readonly draft = signal('');

  protected generateIcon(): Promise<void> {
    return this.store.generateIcon(this.playerName());
  }

  protected async send(): Promise<void> {
    const message = this.draft().trim();
    if (!message) return;
    this.draft.set('');
    await this.store.sendMessage(this.playerName(), message);
  }
}
