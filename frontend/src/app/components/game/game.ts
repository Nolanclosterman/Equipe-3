import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from '../../services/profile.service';
import { ScoreGauge } from '../score-gauge/score-gauge';

/**
 * GAME LOOP screen. Two panels:
 *   - left: the AI help chat (discuss to decide),
 *   - right: the problem with its proposed solutions + illustration.
 * After a decision the narrative consequence and score changes are shown with a
 * "Suivant" button to move to the next event. A funny overlay covers AI waits.
 */
@Component({
  selector: 'app-game',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ScoreGauge],
  templateUrl: './game.html',
  styleUrl: './game.scss',
})
export class Game implements OnDestroy {
  readonly playerName = input.required<string>();

  protected readonly store = inject(ProfileService);
  protected readonly selected = signal<string | null>(null);
  protected readonly draft = signal('');

  protected readonly company = this.store.company;
  protected readonly event = computed(() => this.company()?.currentEvent ?? null);
  protected readonly outcome = computed(() => this.company()?.lastOutcome ?? null);
  protected readonly decided = computed(() => this.outcome() !== null);

  private readonly loadingMessages = [
    "L'IA réfléchit très fort 🤯",
    'On invente un problème rigolo… 🎲',
    'Les investisseurs arrivent 🏃💨',
    'On mélange les idées dans la marmite 🍲',
    'Calcul des super-conséquences ⚡',
  ];
  protected readonly loadingMessage = signal(this.loadingMessages[0]);
  private rotation?: ReturnType<typeof setInterval>;

  constructor() {
    // Rotate the funny loading message while an AI call is in flight.
    effect(() => {
      if (this.store.generating()) {
        this.startRotation();
      } else {
        this.stopRotation();
      }
    });

    // Safety net: if the company is active but has no event yet (e.g. after a
    // backend restart), kick off the first event.
    effect(() => {
      const c = this.company();
      if (
        c?.active &&
        !c.currentEvent &&
        !c.gameOver &&
        !this.store.generating() &&
        !this.store.loading()
      ) {
        this.store.generateEvent(this.playerName());
      }
    });

    // Clear the selection whenever a fresh event arrives.
    effect(() => {
      this.event();
      this.selected.set(null);
    });
  }

  ngOnDestroy(): void {
    this.stopRotation();
  }

  protected async validate(): Promise<void> {
    const choice = this.selected();
    if (!choice || this.decided()) return;
    await this.store.decide(this.playerName(), choice);
  }

  protected next(): Promise<void> {
    return this.store.generateEvent(this.playerName());
  }

  protected async send(): Promise<void> {
    const message = this.draft().trim();
    if (!message) return;
    this.draft.set('');
    await this.store.sendMessage(this.playerName(), message);
  }

  protected delta(value: number): string {
    return value > 0 ? `+${value}` : `${value}`;
  }

  private startRotation(): void {
    let i = 0;
    this.loadingMessage.set(this.loadingMessages[0]);
    this.stopRotation();
    this.rotation = setInterval(() => {
      i = (i + 1) % this.loadingMessages.length;
      this.loadingMessage.set(this.loadingMessages[i]);
    }, 1400);
  }

  private stopRotation(): void {
    if (this.rotation) {
      clearInterval(this.rotation);
      this.rotation = undefined;
    }
  }
}
