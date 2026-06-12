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
  illustration: string;
  lexicon: LexiconEntry[];
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
  character: string | null;
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
