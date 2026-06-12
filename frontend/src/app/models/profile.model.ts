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

export interface Company {
  name: string;
  active: boolean;
  iconUrl: string | null;
  scores: Scores;
  chat: ChatMessage[];
}

export interface Profile {
  name: string;
  /** null when the player has not created a company yet (CAS 1). */
  company: Company | null;
}
