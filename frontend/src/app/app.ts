import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CompanyView } from './components/company/company';
import { Dashboard } from './components/dashboard/dashboard';
import { ProfileService } from './services/profile.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CompanyView, Dashboard],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  protected readonly store = inject(ProfileService);

  /** Connected player. Hard-coded for the base scaffold; wire to auth later. */
  protected readonly playerName = signal('joueur1');

  ngOnInit(): void {
    this.store.loadProfile(this.playerName());
  }
}
