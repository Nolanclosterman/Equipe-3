import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CompanyView } from './components/company/company';
import { Game } from './components/game/game';
import { ProfileService } from './services/profile.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CompanyView, Game],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  protected readonly store = inject(ProfileService);

  /**
   * Connected player. The backend keeps one game runtime per profile name, so
   * each browser gets its own anonymous identity (persisted in localStorage):
   * two visitors on the same deployment never share a game, while a reload in
   * the same browser resumes the existing one. Wire to real auth later.
   */
  protected readonly playerName = signal(App.localPlayerId());

  ngOnInit(): void {
    this.store.loadProfile(this.playerName());
    this.store.loadManifest();
  }

  private static localPlayerId(): string {
    const key = 'equipe3-player-id';
    let id = localStorage.getItem(key);
    if (!id) {
      id = `joueur-${crypto.randomUUID()}`;
      localStorage.setItem(key, id);
    }
    return id;
  }
}
