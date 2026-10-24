/**
 * Robust Local Storage with Schema Validation & Fallback Handling.
 */

const STORAGE_KEY = 'td_rpg_save_v1';

const DEFAULT_PROFILE = {
  version: 1,
  level: 1,
  xp: 0,
  coins: 300,
  unlockedHeroes: ['kael'],
  selectedHeroes: ['kael'],
  unlockedStages: [1],
  completedStages: [],
  settings: {
    soundEnabled: true,
    musicEnabled: true,
    gameSpeed: 1
  },
  lastUpdated: Date.now()
};

class StorageManager {
  constructor(storageKey = STORAGE_KEY) {
    this.storageKey = storageKey;
    this._memoryFallback = null;
  }

  _getStorage() {
    if (typeof window !== 'undefined' && window.localStorage) {
      return window.localStorage;
    }
    return null;
  }

  load() {
    const storage = this._getStorage();
    let raw = null;

    try {
      if (storage) {
        raw = storage.getItem(this.storageKey);
      } else {
        raw = this._memoryFallback;
      }

      if (!raw) {
        return this.createDefault();
      }

      const parsed = JSON.parse(raw);
      return this._validateAndMigrate(parsed);
    } catch (err) {
      console.warn('[StorageManager] Failed to read or parse save. Using default profile safely:', err);
      return this.createDefault();
    }
  }

  save(profile) {
    if (!profile || typeof profile !== 'object') {
      console.error('[StorageManager] Invalid profile provided to save.');
      return false;
    }

    try {
      profile.lastUpdated = Date.now();
      const serialized = JSON.stringify(profile);
      const storage = this._getStorage();

      if (storage) {
        storage.setItem(this.storageKey, serialized);
      } else {
        this._memoryFallback = serialized;
      }
      return true;
    } catch (err) {
      console.error('[StorageManager] Failed to persist profile:', err);
      return false;
    }
  }

  clear() {
    const storage = this._getStorage();
    if (storage) {
      try {
        storage.removeItem(this.storageKey);
      } catch (e) {
        console.error('[StorageManager] Error clearing storage:', e);
      }
    }
    this._memoryFallback = null;
    return true;
  }

  createDefault() {
    const fresh = JSON.parse(JSON.stringify(DEFAULT_PROFILE));
    fresh.lastUpdated = Date.now();
    this.save(fresh);
    return fresh;
  }

  _validateAndMigrate(data) {
    if (!data || typeof data !== 'object') {
      return this.createDefault();
    }

    const profile = JSON.parse(JSON.stringify(DEFAULT_PROFILE));

    if (typeof data.level === 'number' && data.level >= 1) {
      profile.level = Math.floor(data.level);
    }
    if (typeof data.xp === 'number' && data.xp >= 0) {
      profile.xp = Math.floor(data.xp);
    }
    if (typeof data.coins === 'number' && data.coins >= 0) {
      profile.coins = Math.floor(data.coins);
    }
    if (Array.isArray(data.unlockedHeroes) && data.unlockedHeroes.length > 0) {
      profile.unlockedHeroes = Array.from(new Set(data.unlockedHeroes));
    }
    if (Array.isArray(data.selectedHeroes) && data.selectedHeroes.length > 0) {
      // Filter out heroes that are not unlocked
      profile.selectedHeroes = data.selectedHeroes.filter(h => profile.unlockedHeroes.includes(h));
      if (profile.selectedHeroes.length === 0) {
        profile.selectedHeroes = [profile.unlockedHeroes[0]];
      }
    }
    if (Array.isArray(data.unlockedStages) && data.unlockedStages.length > 0) {
      profile.unlockedStages = Array.from(new Set(data.unlockedStages));
    }
    if (Array.isArray(data.completedStages)) {
      profile.completedStages = Array.from(new Set(data.completedStages));
    }
    if (data.settings && typeof data.settings === 'object') {
      profile.settings.soundEnabled = data.settings.soundEnabled !== false;
      profile.settings.musicEnabled = data.settings.musicEnabled !== false;
      profile.settings.gameSpeed = [1, 2].includes(data.settings.gameSpeed) ? data.settings.gameSpeed : 1;
    }

    return profile;
  }
}

if (typeof module !== 'undefined') {
  module.exports = { StorageManager, DEFAULT_PROFILE };
}
