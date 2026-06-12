/**
 * Catalog of the one-shot generated assets (see OneShotAssetsService on the
 * backend). Each entry pairs the asset key of the generated PNG with an emoji
 * fallback used while the image does not exist yet (no OpenAI key, or the
 * one-shot generation is still running).
 */

export interface AvatarChoice {
  /** Asset key of the generated profile picture (avatar-1 … avatar-8). */
  key: string;
  /** Player-facing name, also sent to the AI as the player's avatar. */
  label: string;
  emoji: string;
}

export interface TypeChoice {
  /** Asset key of the generated activity icon (type-…). */
  key: string;
  label: string;
  emoji: string;
  /** Suggested company name when picking this activity. */
  sample: string;
}

export const AVATARS: AvatarChoice[] = [
  { key: 'avatar-1', label: 'Tom', emoji: '👦' },
  { key: 'avatar-2', label: 'Léa', emoji: '👧' },
  { key: 'avatar-3', label: 'Noah', emoji: '🧒' },
  { key: 'avatar-4', label: 'Mei', emoji: '👧' },
  { key: 'avatar-5', label: 'Max', emoji: '👦' },
  { key: 'avatar-6', label: 'Awa', emoji: '👧' },
  { key: 'avatar-7', label: 'Sacha', emoji: '🧒' },
  { key: 'avatar-8', label: 'Emma', emoji: '👧' },
];

export const TYPES: TypeChoice[] = [
  { key: 'type-resto', label: 'Resto rigolo', emoji: '🍔', sample: 'Burger Galaxie' },
  { key: 'type-jeux-video', label: 'Jeux vidéo', emoji: '🎮', sample: 'Pixel Power' },
  { key: 'type-mode', label: 'Mode', emoji: '👕', sample: 'Super Style' },
  { key: 'type-robots', label: 'Robots', emoji: '🤖', sample: 'RoboCopains' },
  { key: 'type-eco', label: 'Éco & nature', emoji: '🌱', sample: 'Planète Verte' },
  { key: 'type-techno', label: 'Techno', emoji: '🚀', sample: 'Fusée Tech' },
];

/** Finds the avatar entry from the stored character label (or null). */
export function avatarByLabel(label: string | null): AvatarChoice | null {
  return AVATARS.find((a) => a.label === label) ?? null;
}
