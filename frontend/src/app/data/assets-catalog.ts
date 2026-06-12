/**
 * Catalog of the UI assets. Avatars are static images shipped with the
 * frontend (public/avatars/). Type/UI icons are one-shot generated assets
 * (see OneShotAssetsService on the backend); each entry pairs the asset key
 * of the generated PNG with an emoji fallback used while the image does not
 * exist yet (no OpenAI key, or the one-shot generation is still running).
 */

export interface AvatarChoice {
  /** Stable key stored on the backend (avatar-1 … avatar-8). */
  key: string;
  /** Suggested avatar name (the player can type their own). */
  label: string;
  emoji: string;
  /** Static profile picture bundled with the frontend (public/avatars/). */
  image: string;
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
  { key: 'avatar-1', label: 'Tom', emoji: '👦', image: 'avatars/avatar-1.png' },
  { key: 'avatar-2', label: 'Léa', emoji: '👧', image: 'avatars/avatar-2.png' },
  { key: 'avatar-3', label: 'Noah', emoji: '🧒', image: 'avatars/avatar-3.png' },
  { key: 'avatar-4', label: 'Mei', emoji: '👧', image: 'avatars/avatar-4.png' },
  { key: 'avatar-5', label: 'Max', emoji: '👦', image: 'avatars/avatar-5.png' },
  { key: 'avatar-6', label: 'Awa', emoji: '👧', image: 'avatars/avatar-6.png' },
  { key: 'avatar-7', label: 'Sacha', emoji: '🧒', image: 'avatars/avatar-7.png' },
  { key: 'avatar-8', label: 'Emma', emoji: '👧', image: 'avatars/avatar-8.png' },
];

export const TYPES: TypeChoice[] = [
  { key: 'type-resto', label: 'Resto rigolo', emoji: '🍔', sample: 'Burger Galaxie' },
  { key: 'type-jeux-video', label: 'Jeux vidéo', emoji: '🎮', sample: 'Pixel Power' },
  { key: 'type-mode', label: 'Mode', emoji: '👕', sample: 'Super Style' },
  { key: 'type-robots', label: 'Robots', emoji: '🤖', sample: 'RoboCopains' },
  { key: 'type-eco', label: 'Éco & nature', emoji: '🌱', sample: 'Planète Verte' },
  { key: 'type-techno', label: 'Techno', emoji: '🚀', sample: 'Fusée Tech' },
];

/** Finds the avatar entry from its asset key (or null). */
export function avatarByKey(key: string | null): AvatarChoice | null {
  return AVATARS.find((a) => a.key === key) ?? null;
}
