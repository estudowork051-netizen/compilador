const { EventEmitter } = require('./events');
const { StorageManager } = require('./storage');
const { PROGRESSION_TABLE, HEROES } = require('../game/config');

class GameState extends EventEmitter {
  constructor(storageManager = new StorageManager()) {
    super();
    this.storage = storageManager;
    this.profile = this.storage.load();
    this._ensureProgressionIntegrity();
  }

  _ensureProgressionIntegrity() {
    // Check if player level qualifies for any ungranted unlocks from progression table
    let changed = false;
    for (const row of PROGRESSION_TABLE) {
      if (this.profile.level >= row.level && row.unlockHero) {
        if (!this.profile.unlockedHeroes.includes(row.unlockHero)) {
          this.profile.unlockedHeroes.push(row.unlockHero);
          changed = true;
        }
      }
    }
    if (changed) {
      this.storage.save(this.profile);
    }
  }

  getProfile() {
    return this.profile;
  }

  getMaxHeroSlots() {
    let slots = 1;
    for (const row of PROGRESSION_TABLE) {
      if (this.profile.level >= row.level) {
        slots = row.slots;
      }
    }
    return slots;
  }

  addXp(amount) {
    if (typeof amount !== 'number' || amount <= 0) return { levelUps: 0, newLevel: this.profile.level };
    this.profile.xp += Math.floor(amount);
    let levelUps = 0;

    // Check progression table for level ups
    while (true) {
      const nextLevelRow = PROGRESSION_TABLE.find(r => r.level === this.profile.level + 1);
      if (!nextLevelRow) break; // Max level reached

      if (this.profile.xp >= nextLevelRow.xpRequired) {
        this.profile.level += 1;
        levelUps += 1;

        // Rewards
        if (nextLevelRow.rewardGold > 0) {
          this.profile.coins += nextLevelRow.rewardGold;
        }

        // Automatic hero unlock if specified in progression table
        if (nextLevelRow.unlockHero && !this.profile.unlockedHeroes.includes(nextLevelRow.unlockHero)) {
          this.profile.unlockedHeroes.push(nextLevelRow.unlockHero);
          this.emit('hero_unlocked', { heroId: nextLevelRow.unlockHero, reason: 'level_up' });
        }

        this.emit('level_up', {
          level: this.profile.level,
          slots: nextLevelRow.slots,
          rewardGold: nextLevelRow.rewardGold,
          unlockedHero: nextLevelRow.unlockHero
        });
      } else {
        break;
      }
    }

    this.storage.save(this.profile);
    this.emit('profile_updated', this.profile);
    return { levelUps, newLevel: this.profile.level };
  }

  addCoins(amount) {
    if (typeof amount !== 'number' || amount <= 0) return this.profile.coins;
    this.profile.coins += Math.floor(amount);
    this.storage.save(this.profile);
    this.emit('coins_changed', { coins: this.profile.coins, delta: amount });
    this.emit('profile_updated', this.profile);
    return this.profile.coins;
  }

  spendCoins(amount) {
    if (typeof amount !== 'number' || amount <= 0) return false;
    if (this.profile.coins < amount) return false;

    this.profile.coins -= Math.floor(amount);
    this.storage.save(this.profile);
    this.emit('coins_changed', { coins: this.profile.coins, delta: -amount });
    this.emit('profile_updated', this.profile);
    return true;
  }

  unlockHero(heroId) {
    if (!HEROES[heroId]) {
      return { success: false, reason: 'invalid_hero' };
    }
    if (this.profile.unlockedHeroes.includes(heroId)) {
      return { success: false, reason: 'already_unlocked' };
    }

    this.profile.unlockedHeroes.push(heroId);
    this.storage.save(this.profile);
    this.emit('hero_unlocked', { heroId, reason: 'manual' });
    this.emit('profile_updated', this.profile);
    return { success: true };
  }

  selectHero(heroId) {
    if (!this.profile.unlockedHeroes.includes(heroId)) {
      return { success: false, reason: 'hero_not_unlocked' };
    }
    if (this.profile.selectedHeroes.includes(heroId)) {
      return { success: true, message: 'already_selected' };
    }

    const maxSlots = this.getMaxHeroSlots();
    if (this.profile.selectedHeroes.length >= maxSlots) {
      return { success: false, reason: 'slots_full', maxSlots };
    }

    this.profile.selectedHeroes.push(heroId);
    this.storage.save(this.profile);
    this.emit('roster_changed', { selectedHeroes: [...this.profile.selectedHeroes] });
    this.emit('profile_updated', this.profile);
    return { success: true };
  }

  deselectHero(heroId) {
    if (!this.profile.selectedHeroes.includes(heroId)) {
      return { success: false, reason: 'not_selected' };
    }
    if (this.profile.selectedHeroes.length <= 1) {
      return { success: false, reason: 'cannot_empty_party' };
    }

    this.profile.selectedHeroes = this.profile.selectedHeroes.filter(id => id !== heroId);
    this.storage.save(this.profile);
    this.emit('roster_changed', { selectedHeroes: [...this.profile.selectedHeroes] });
    this.emit('profile_updated', this.profile);
    return { success: true };
  }

  completeStage(stageId, stars = 3) {
    if (!this.profile.completedStages.includes(stageId)) {
      this.profile.completedStages.push(stageId);
    }
    const nextStageId = stageId + 1;
    if (!this.profile.unlockedStages.includes(nextStageId)) {
      this.profile.unlockedStages.push(nextStageId);
    }
    this.storage.save(this.profile);
    this.emit('stage_completed', { stageId, nextStageId });
    this.emit('profile_updated', this.profile);
  }

  updateSettings(settingsPartial) {
    this.profile.settings = { ...this.profile.settings, ...settingsPartial };
    this.storage.save(this.profile);
    this.emit('settings_changed', this.profile.settings);
  }

  resetProgress() {
    this.profile = this.storage.createDefault();
    this.emit('profile_reset', this.profile);
    this.emit('profile_updated', this.profile);
    return this.profile;
  }
}

if (typeof module !== 'undefined') {
  module.exports = { GameState };
}
