/** Shapes returned by the Spring Boot API (see ProfileController). */

export interface Scores {
  money: number;
  ecology: number;
  ethics: number;
}

export interface ChatMessage {
  author: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

export interface LexiconEntry {
  term: string;
  definition: string;
}

/** A generated event, matching prompts/event_generation.md. */
export interface GameEvent {
  problem: string;
  solutions: string[];
  character: string;
  /** Portrait of the presenting character; '' until generated. */
  characterImage: string;
  /** Landscape illustration of the problem; '' until generated. */
  illustration: string;
  /** One vignette per solution (same order); '' until generated. */
  solutionIllustrations: string[];
  lexicon: LexiconEntry[];
}

/** GET /assets/manifest — the one-shot assets (avatars, UI icons). */
export interface AssetManifest {
  /** False when the backend has no OpenAI key (no images will ever come). */
  enabled: boolean;
  /** asset key (e.g. 'avatar-1', 'type-resto', 'ui-loader') -> image URL. */
  assets: Record<string, string>;
}

/** Narrative consequence + indicator deltas, from prompts/event_scoring.md. */
export interface EventOutcome {
  narrative: string;
  moneyDelta: number;
  ecologyDelta: number;
  ethicsDelta: number;
}

export interface Company {
  name: string;
  type: string | null;
  /** Name the player gave to their avatar. */
  character: string | null;
  /** One-shot asset key of the chosen avatar picture (e.g. 'avatar-3'). */
  avatar: string | null;
  active: boolean;
  iconUrl: string | null;
  scores: Scores;
  chat: ChatMessage[];
  currentEvent: GameEvent | null;
  lastOutcome: EventOutcome | null;
  eventNumber: number;
  maxEvents: number;
  gameOver: boolean;
}

export interface Profile {
  name: string;
  /** null when the player has not created a company yet (CAS 1). */
  company: Company | null;
}
