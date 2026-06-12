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
import { GameEvent } from '../../models/profile.model';
import { ProfileService } from '../../services/profile.service';
import { ScoreGauge } from '../score-gauge/score-gauge';

/** Sentinel for "the player writes their own solution" card. */
const CUSTOM = '__custom__';

/**
 * GAME LOOP screen, following mockups/img_1.png:
 *   - left: the AI assistant chat,
 *   - center: the illustrated problem presented by a character,
 *   - right: the LEXIQUE panel (mots à connaître),
 *   - bottom: one illustrated card per solution + a card where the player can
 *     propose their own idea.
 * After a decision the narrative consequence and score changes are shown with a
 * "Tour suivant" button. A funny overlay covers AI waits, and the screen polls
 * the backend while the event images are being generated.
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

  protected readonly CUSTOM = CUSTOM;
  protected readonly store = inject(ProfileService);
  /** A proposed solution, the CUSTOM sentinel, or null. */
  protected readonly selected = signal<string | null>(null);
  protected readonly customSolution = signal('');
  protected readonly draft = signal('');

  protected readonly company = this.store.company;
  protected readonly event = computed(() => this.company()?.currentEvent ?? null);
  protected readonly outcome = computed(() => this.company()?.lastOutcome ?? null);
  protected readonly decided = computed(() => this.outcome() !== null);

  protected readonly canValidate = computed(() => {
    const choice = this.selected();
    if (!choice || this.decided()) return false;
    return choice !== CUSTOM || this.customSolution().trim().length > 0;
  });

  private readonly loadingMessages = [
    "L'IA réfléchit très fort 🤯",
    'On invente un problème rigolo… 🎲',
    'Les investisseurs arrivent 🏃💨',
    'On mélange les idées dans la marmite 🍲',
    'Calcul des super-conséquences ⚡',
  ];
  protected readonly loadingMessage = signal(this.loadingMessages[0]);
  private rotation?: ReturnType<typeof setInterval>;

  /** Polls the profile while the event images are generated in the background. */
  private imagePolling?: ReturnType<typeof setInterval>;
  private polledProblem = '';
  private pollsLeft = 0;

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

    // Clear the selections whenever a fresh event arrives (not on the silent
    // image-polling refreshes, which keep the same problem text).
    effect(() => {
      const problem = this.event()?.problem ?? '';
      if (problem !== this.polledProblem) {
        this.polledProblem = problem;
        this.pollsLeft = 48; // ~2 minutes of polling per event
        this.selected.set(null);
        this.customSolution.set('');
      }
      this.syncImagePolling();
    });
  }

  ngOnDestroy(): void {
    this.stopRotation();
    this.stopImagePolling();
  }

  protected async validate(): Promise<void> {
    if (!this.canValidate()) return;
    const choice = this.selected();
    const solution = choice === CUSTOM ? this.customSolution().trim() : choice!;
    await this.store.decide(this.playerName(), solution);
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

  // ------------------------------------------------------- image polling

  private missingImages(event: GameEvent): boolean {
    return (
      !event.illustration ||
      (!!event.character && !event.characterImage) ||
      event.solutionIllustrations.some((url) => !url)
    );
  }

  private syncImagePolling(): void {
    const event = this.event();
    const shouldPoll =
      this.store.imagesEnabled() && !!event && this.missingImages(event) && this.pollsLeft > 0;
    if (shouldPoll && !this.imagePolling) {
      this.imagePolling = setInterval(() => {
        this.pollsLeft--;
        this.store.refreshQuietly(this.playerName());
        const current = this.event();
        if (this.pollsLeft <= 0 || !current || !this.missingImages(current)) {
          this.stopImagePolling();
        }
      }, 2500);
    } else if (!shouldPoll) {
      this.stopImagePolling();
    }
  }

  private stopImagePolling(): void {
    if (this.imagePolling) {
      clearInterval(this.imagePolling);
      this.imagePolling = undefined;
    }
  }

  // ------------------------------------------------------ loader rotation

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
