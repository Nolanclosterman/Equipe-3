import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Circular gauge showing one indicator out of 100, matching the dashboard
 * dials of the mockup (Score argent / écologie / éthique).
 */
@Component({
  selector: 'app-score-gauge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="gauge">
      <svg viewBox="0 0 120 120" class="ring">
        <circle class="track" cx="60" cy="60" r="52" />
        <circle
          class="value"
          cx="60"
          cy="60"
          r="52"
          [style.stroke]="color()"
          [style.stroke-dasharray]="circumference()"
          [style.stroke-dashoffset]="offset()"
        />
      </svg>
      <div class="reading">
        <span class="icon">{{ icon() }}</span>
        <span class="number">{{ value() }}</span>
        <span class="max">/100</span>
      </div>
    </div>
    <p class="label">{{ label() }}</p>
  `,
  styleUrl: './score-gauge.scss',
})
export class ScoreGauge {
  readonly label = input.required<string>();
  readonly icon = input<string>('⭐');
  readonly value = input.required<number>();
  readonly color = input<string>('#3b82f6');

  protected readonly circumference = computed(() => 2 * Math.PI * 52);
  protected readonly offset = computed(() => {
    const clamped = Math.max(0, Math.min(100, this.value()));
    return this.circumference() * (1 - clamped / 100);
  });
}
