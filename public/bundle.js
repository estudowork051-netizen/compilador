/**
 * BUNDLED BROWSER / ANDROID WEBVIEW RUNTIME
 * Generated automatically by RPG Studio Bundler
 */
(function() {
  'use strict';
  const modules = {
  "core/events": function(module, exports, require) {
/**
 * Lightweight Event Emitter for decoupled module communication.
 */
class EventEmitter {
  constructor() {
    this._events = new Map();
  }

  on(event, handler) {
    if (typeof handler !== 'function') return this;
    if (!this._events.has(event)) {
      this._events.set(event, []);
    }
    this._events.get(event).push(handler);
    return this;
  }

  off(event, handler) {
    if (!this._events.has(event)) return this;
    if (!handler) {
      this._events.delete(event);
      return this;
    }
    const handlers = this._events.get(event);
    const idx = handlers.indexOf(handler);
    if (idx !== -1) {
      handlers.splice(idx, 1);
    }
    if (handlers.length === 0) {
      this._events.delete(event);
    }
    return this;
  }

  once(event, handler) {
    const wrapper = (...args) => {
      this.off(event, wrapper);
      handler.apply(this, args);
    };
    return this.on(event, wrapper);
  }

  emit(event, ...args) {
    if (!this._events.has(event)) return false;
    const handlers = [...this._events.get(event)];
    for (const h of handlers) {
      try {
        h(...args);
      } catch (err) {
        console.error(`[EventEmitter] Error in event '${event}':`, err);
      }
    }
    return true;
  }

  clear() {
    this._events.clear();
  }
}

if (typeof module !== 'undefined') {
  module.exports = { EventEmitter };
}

  },

  "core/storage": function(module, exports, require) {
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

  },

  "game/config": function(module, exports, require) {
/**
 * Game Configuration & Design Data
 */

const RARITY = {
  COMMON: { id: 'common', name: 'Comum', color: '#9e9e9e', border: '#757575', badge: '✦' },
  RARE: { id: 'rare', name: 'Raro', color: '#29b6f6', border: '#0288d1', badge: '✦✦' },
  EPIC: { id: 'epic', name: 'Épico', color: '#ab47bc', border: '#7b1fa2', badge: '✦✦✦' },
  LEGENDARY: { id: 'legendary', name: 'Lendário', color: '#ffa726', border: '#f57c00', badge: '✦✦✦✦' }
};

const HEROES = {
  kael: {
    id: 'kael',
    name: 'Kael',
    title: 'Guardião Arcano',
    rarity: 'COMMON',
    role: 'Guardião',
    description: 'Equilibrado e focado em controle com magia arcana protetora.',
    stats: { hp: 500, attack: 45, defense: 30, skillPower: 150 },
    skill: {
      id: 'arcane_blast',
      name: 'Explosão Arcana',
      description: 'Causa explosão de energia mágica em área (150 de dano mágico).',
      cooldown: 12,
      damage: 150,
      radius: 180,
      type: 'aoe_damage'
    },
    unlockLevel: 1,
    cost: 0,
    avatar: '🧙‍♂️',
    iconColor: '#3f51b5'
  },
  lyra: {
    id: 'lyra',
    name: 'Lyra',
    title: 'Arqueira Astral',
    rarity: 'RARE',
    role: 'Atiradora',
    description: 'Especialista em saraivadas de precisão e dano à distância.',
    stats: { hp: 380, attack: 65, defense: 18, skillPower: 200 },
    skill: {
      id: 'arrow_rain',
      name: 'Chuva de Flechas',
      description: 'Dispara uma tempestade de flechas astrais (200 de dano em área).',
      cooldown: 14,
      damage: 200,
      radius: 200,
      type: 'aoe_damage'
    },
    unlockLevel: 3,
    cost: 250,
    avatar: '🏹',
    iconColor: '#00acc1'
  },
  brakk: {
    id: 'brakk',
    name: 'Brakk',
    title: 'Guerreiro das Rochas',
    rarity: 'RARE',
    role: 'Defensor',
    description: 'Guerreiro resistente que abala o solo com força titânica.',
    stats: { hp: 750, attack: 50, defense: 50, skillPower: 120 },
    skill: {
      id: 'seismic_slam',
      name: 'Impacto Sísmico',
      description: 'Atinge o solo, causando 120 de dano e atordoando inimigos por 3s.',
      cooldown: 16,
      damage: 120,
      radius: 190,
      stunDuration: 3,
      type: 'stun_aoe'
    },
    unlockLevel: 5,
    cost: 400,
    avatar: '🛡️',
    iconColor: '#8d6e63'
  },
  nyra: {
    id: 'nyra',
    name: 'Nyra',
    title: 'Maga das Sombras',
    rarity: 'EPIC',
    role: 'Maga',
    description: 'Manipuladora da escuridão capaz de drenar o avanço inimigo.',
    stats: { hp: 420, attack: 60, defense: 22, skillPower: 180 },
    skill: {
      id: 'shadow_field',
      name: 'Campo de Lentidão',
      description: 'Invoca névoa sombria que reduz a velocidade dos inimigos em 60% por 6s.',
      cooldown: 18,
      damage: 80,
      radius: 240,
      slowFactor: 0.40,
      duration: 6,
      type: 'slow_field'
    },
    unlockLevel: 7,
    cost: 650,
    avatar: '🔮',
    iconColor: '#7e57c2'
  },
  tiko: {
    id: 'tiko',
    name: 'Tiko',
    title: 'Engenheiro de Batalha',
    rarity: 'EPIC',
    role: 'Suporte',
    description: 'Gênio da artilharia que potencializa as defesas no campo.',
    stats: { hp: 440, attack: 40, defense: 28, skillPower: 140 },
    skill: {
      id: 'overcharge',
      name: 'Sobrecarga das Torres',
      description: 'Aumenta o dano em +40% e a velocidade de ataque de todas as torres em +60% por 7s.',
      cooldown: 20,
      duration: 7,
      damageBuff: 0.40,
      speedBuff: 0.60,
      type: 'tower_buff'
    },
    unlockLevel: 9,
    cost: 900,
    avatar: '⚙️',
    iconColor: '#ff7043'
  },
  vesper: {
    id: 'vesper',
    name: 'Vesper',
    title: 'Guardiã do Crepúsculo',
    rarity: 'LEGENDARY',
    role: 'Mística',
    description: 'Entidade cósmica que rasga a realidade aniquilando hordas inteiras.',
    stats: { hp: 600, attack: 95, defense: 35, skillPower: 450 },
    skill: {
      id: 'dimensional_rift',
      name: 'Ruptura Dimensional',
      description: 'Rasga o espaço-tempo causando 450 de dano maciço a todos os inimigos.',
      cooldown: 25,
      damage: 450,
      radius: 9999,
      type: 'screen_aoe'
    },
    unlockLevel: 10,
    cost: 1500,
    avatar: '✨',
    iconColor: '#ffd700'
  }
};

const TOWERS = {
  archer: {
    id: 'archer',
    name: 'Torre Arqueira',
    type: 'archer',
    avatar: '🏹',
    color: '#4caf50',
    description: 'Ataques rápidos e consistentes com longo alcance.',
    levels: [
      { level: 1, cost: 100, range: 135, damage: 20, fireRate: 1.4, projectileSpeed: 420 },
      { level: 2, cost: 120, range: 155, damage: 32, fireRate: 1.6, projectileSpeed: 460 },
      { level: 3, cost: 180, range: 175, damage: 48, fireRate: 1.9, projectileSpeed: 500 }
    ]
  },
  magic: {
    id: 'magic',
    name: 'Torre Mágica',
    type: 'magic',
    avatar: '🔮',
    color: '#9c27b0',
    description: 'Dispara orbes arcanos que perfuram a armadura inimiga.',
    levels: [
      { level: 1, cost: 140, range: 115, damage: 42, fireRate: 0.85, projectileSpeed: 340, armorPen: 0.5 },
      { level: 2, cost: 160, range: 135, damage: 68, fireRate: 1.05, projectileSpeed: 380, armorPen: 0.7 },
      { level: 3, cost: 220, range: 155, damage: 105, fireRate: 1.25, projectileSpeed: 420, armorPen: 1.0 }
    ]
  },
  cannon: {
    id: 'cannon',
    name: 'Torre de Canhão',
    type: 'cannon',
    avatar: '💣',
    color: '#e65100',
    description: 'Disparos explosivos com dano em área contra grupos.',
    levels: [
      { level: 1, cost: 160, range: 120, damage: 55, fireRate: 0.55, splashRadius: 65, projectileSpeed: 260 },
      { level: 2, cost: 190, range: 140, damage: 85, fireRate: 0.70, splashRadius: 80, projectileSpeed: 290 },
      { level: 3, cost: 250, range: 160, damage: 130, fireRate: 0.85, splashRadius: 95, projectileSpeed: 320 }
    ]
  }
};

const ENEMIES = {
  goblin: {
    id: 'goblin',
    name: 'Goblin Ladrão',
    hp: 70,
    speed: 85,
    reward: 15,
    xp: 12,
    baseDamage: 1,
    size: 14,
    color: '#8bc34a',
    armor: 0
  },
  orc: {
    id: 'orc',
    name: 'Orc Guerreiro',
    hp: 160,
    speed: 55,
    reward: 25,
    xp: 22,
    baseDamage: 1,
    size: 18,
    color: '#689f38',
    armor: 5
  },
  golem: {
    id: 'golem',
    name: 'Golem de Pedra',
    hp: 380,
    speed: 36,
    reward: 45,
    xp: 38,
    baseDamage: 2,
    size: 22,
    color: '#795548',
    armor: 15
  },
  ogre_boss: {
    id: 'ogre_boss',
    name: 'Ogro das Ruínas',
    title: 'Devastador Ancestral',
    hp: 1700,
    speed: 30,
    reward: 350,
    xp: 350,
    baseDamage: 5,
    size: 32,
    color: '#b71c1c',
    armor: 20,
    isBoss: true,
    enrageHpRatio: 0.40,
    enrageSpeedMultiplier: 1.45,
    enrageDamageMultiplier: 1.50
  }
};

const PROGRESSION_TABLE = [
  { level: 1, xpRequired: 0, slots: 1, unlockHero: 'kael', rewardGold: 0 },
  { level: 2, xpRequired: 150, slots: 1, unlockHero: null, rewardGold: 100 },
  { level: 3, xpRequired: 350, slots: 2, unlockHero: 'lyra', rewardGold: 150 },
  { level: 4, xpRequired: 650, slots: 2, unlockHero: null, rewardGold: 200 },
  { level: 5, xpRequired: 1050, slots: 3, unlockHero: 'brakk', rewardGold: 250 },
  { level: 6, xpRequired: 1550, slots: 3, unlockHero: null, rewardGold: 300 },
  { level: 7, xpRequired: 2150, slots: 4, unlockHero: 'nyra', rewardGold: 350 },
  { level: 8, xpRequired: 2850, slots: 4, unlockHero: null, rewardGold: 400 },
  { level: 9, xpRequired: 3650, slots: 5, unlockHero: 'tiko', rewardGold: 500 },
  { level: 10, xpRequired: 4600, slots: 5, unlockHero: 'vesper', rewardGold: 1000 }
];

const STAGES = [
  {
    id: 1,
    name: 'Floresta Encantada',
    theme: 'forest',
    description: 'Entrada do reino florestal infestada por batedores e saqueadores.',
    baseHp: 20,
    startingGold: 250,
    rewardGold: 200,
    rewardXp: 180,
    waves: [
      { enemies: [{ type: 'goblin', count: 6, interval: 1.4 }] },
      { enemies: [{ type: 'goblin', count: 8, interval: 1.2 }, { type: 'orc', count: 2, interval: 2.0 }] },
      { enemies: [{ type: 'orc', count: 5, interval: 1.8 }, { type: 'goblin', count: 6, interval: 1.0 }] },
      { enemies: [{ type: 'orc', count: 7, interval: 1.5 }, { type: 'golem', count: 1, interval: 3.0 }] },
      { enemies: [{ type: 'goblin', count: 10, interval: 0.9 }, { type: 'golem', count: 2, interval: 2.5 }] }
    ],
    waypoints: [
      { x: 270, y: 30 },
      { x: 270, y: 150 },
      { x: 100, y: 150 },
      { x: 100, y: 330 },
      { x: 440, y: 330 },
      { x: 440, y: 520 },
      { x: 270, y: 520 },
      { x: 270, y: 690 }
    ],
    buildSpots: [
      { id: 'b1', x: 185, y: 100 },
      { id: 'b2', x: 355, y: 100 },
      { id: 'b3', x: 185, y: 240 },
      { id: 'b4', x: 355, y: 240 },
      { id: 'b5', x: 185, y: 425 },
      { id: 'b6', x: 355, y: 425 },
      { id: 'b7', x: 185, y: 605 },
      { id: 'b8', x: 355, y: 605 }
    ]
  },
  {
    id: 2,
    name: 'Ruínas Antigas',
    theme: 'ruins',
    description: 'Cidadela abandonada onde repousa o temido Ogro das Ruínas.',
    baseHp: 20,
    startingGold: 300,
    rewardGold: 400,
    rewardXp: 450,
    hasBoss: true,
    bossEnemyId: 'ogre_boss',
    waves: [
      { enemies: [{ type: 'goblin', count: 8, interval: 1.2 }] },
      { enemies: [{ type: 'orc', count: 6, interval: 1.4 }, { type: 'goblin', count: 4, interval: 1.0 }] },
      { enemies: [{ type: 'golem', count: 2, interval: 2.5 }, { type: 'orc', count: 6, interval: 1.2 }] },
      { enemies: [{ type: 'goblin', count: 12, interval: 0.8 }, { type: 'golem', count: 3, interval: 2.0 }] },
      { enemies: [{ type: 'orc', count: 10, interval: 1.2 }, { type: 'golem', count: 3, interval: 2.0 }] },
      { enemies: [{ type: 'ogre_boss', count: 1, interval: 0 }] }
    ],
    waypoints: [
      { x: 120, y: 30 },
      { x: 120, y: 200 },
      { x: 420, y: 200 },
      { x: 420, y: 400 },
      { x: 120, y: 400 },
      { x: 120, y: 600 },
      { x: 270, y: 600 },
      { x: 270, y: 690 }
    ],
    buildSpots: [
      { id: 'b1', x: 270, y: 130 },
      { id: 'b2', x: 270, y: 270 },
      { id: 'b3', x: 270, y: 470 },
      { id: 'b4', x: 40, y: 300 },
      { id: 'b5', x: 490, y: 300 },
      { id: 'b6', x: 200, y: 340 },
      { id: 'b7', x: 340, y: 340 },
      { id: 'b8', x: 390, y: 550 }
    ]
  },
  {
    id: 3,
    name: 'Terras Vulcânicas',
    theme: 'volcano',
    description: 'Solo ígneo de magma fervente guardado por gigantes de rocha.',
    baseHp: 25,
    startingGold: 350,
    rewardGold: 600,
    rewardXp: 700,
    waves: [
      { enemies: [{ type: 'goblin', count: 12, interval: 0.8 }] },
      { enemies: [{ type: 'orc', count: 10, interval: 1.0 }, { type: 'golem', count: 2, interval: 2.0 }] },
      { enemies: [{ type: 'golem', count: 4, interval: 1.8 }, { type: 'orc', count: 8, interval: 1.0 }] },
      { enemies: [{ type: 'goblin', count: 15, interval: 0.6 }, { type: 'golem', count: 4, interval: 1.5 }] },
      { enemies: [{ type: 'orc', count: 14, interval: 0.9 }, { type: 'golem', count: 5, interval: 1.6 }] }
    ],
    waypoints: [
      { x: 420, y: 30 },
      { x: 420, y: 170 },
      { x: 120, y: 170 },
      { x: 120, y: 370 },
      { x: 380, y: 370 },
      { x: 380, y: 560 },
      { x: 270, y: 560 },
      { x: 270, y: 690 }
    ],
    buildSpots: [
      { id: 'b1', x: 270, y: 100 },
      { id: 'b2', x: 250, y: 260 },
      { id: 'b3', x: 40, y: 260 },
      { id: 'b4', x: 250, y: 460 },
      { id: 'b5', x: 470, y: 460 },
      { id: 'b6', x: 160, y: 460 },
      { id: 'b7', x: 180, y: 630 },
      { id: 'b8', x: 360, y: 630 }
    ]
  }
];

if (typeof module !== 'undefined') {
  module.exports = {
    RARITY,
    HEROES,
    TOWERS,
    ENEMIES,
    PROGRESSION_TABLE,
    STAGES
  };
}

  },

  "core/state": function(module, exports, require) {
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

  },

  "core/router": function(module, exports, require) {
const { EventEmitter } = require('./events');

/**
 * Robust Central Navigation Router with History Stack and Lifecycle Management.
 * Solves the navigation failures of the previous project completely.
 */
class ScreenRouter extends EventEmitter {
  constructor(options = {}) {
    super();
    this.routes = new Map();
    this.history = [];
    this.currentRoute = null;
    this.currentScreen = null;
    this.defaultRoute = options.defaultRoute || 'main_menu';
    this.domContainer = options.domContainer || null;
  }

  registerRoute(name, screen) {
    if (!name || typeof name !== 'string') {
      throw new Error(`[Router] Route name must be a non-empty string.`);
    }
    if (!screen || typeof screen !== 'object') {
      throw new Error(`[Router] Screen handler for '${name}' must be an object.`);
    }
    this.routes.set(name, screen);
    if (typeof screen.init === 'function') {
      try {
        screen.init(this);
      } catch (err) {
        console.error(`[Router] Error initializing screen '${name}':`, err);
      }
    }
    return this;
  }

  hasRoute(name) {
    return this.routes.has(name);
  }

  getCurrentRoute() {
    return this.currentRoute;
  }

  getHistory() {
    return [...this.history];
  }

  navigate(routeName, params = {}) {
    if (!this.routes.has(routeName)) {
      console.error(`[Router] Unknown destination: '${routeName}'. Aborting navigation.`);
      return false;
    }

    if (this.currentRoute === routeName && !params.forceReload) {
      // Already on this route
      return true;
    }

    const previousRoute = this.currentRoute;
    const previousScreen = this.currentScreen;
    const nextScreen = this.routes.get(routeName);

    // Call exit on previous screen if it exists
    if (previousScreen && typeof previousScreen.onExit === 'function') {
      try {
        previousScreen.onExit();
      } catch (err) {
        console.error(`[Router] Error exiting screen '${previousRoute}':`, err);
      }
    }

    // Push to history if navigating from a valid route and not marked as replace
    if (previousRoute && !params.replace) {
      this.history.push({ route: previousRoute, params: params.previousParams || {} });
    }

    this.currentRoute = routeName;
    this.currentScreen = nextScreen;

    // DOM UI synchronization if available
    this._syncDom(routeName);

    // Call enter lifecycle
    if (typeof nextScreen.onEnter === 'function') {
      try {
        nextScreen.onEnter(params);
      } catch (err) {
        console.error(`[Router] Error entering screen '${routeName}':`, err);
      }
    }

    this.emit('route_changed', {
      current: routeName,
      previous: previousRoute,
      historyDepth: this.history.length,
      params
    });

    return true;
  }

  replace(routeName, params = {}) {
    return this.navigate(routeName, { ...params, replace: true });
  }

  back() {
    if (this.history.length > 0) {
      const prev = this.history.pop();
      return this.navigate(prev.route, { ...prev.params, replace: true });
    }
    // Fallback if history is empty
    if (this.currentRoute !== this.defaultRoute) {
      return this.navigate(this.defaultRoute, { replace: true });
    }
    return false;
  }

  clearHistory() {
    this.history = [];
  }

  _syncDom(activeRoute) {
    if (typeof document === 'undefined') return;

    // Remove active and show states from all registered screens
    const allScreens = document.querySelectorAll('.app-screen');
    allScreens.forEach(el => {
      el.classList.remove('active');
      el.setAttribute('aria-hidden', 'true');
      el.style.display = 'none'; // Guarantee no invisible panel intercepts touches
      el.style.pointerEvents = 'none';
    });

    // Activate the targeted screen
    const targetEl = document.getElementById(`screen-${activeRoute}`);
    if (targetEl) {
      targetEl.classList.add('active');
      targetEl.setAttribute('aria-hidden', 'false');
      targetEl.style.display = 'flex';
      targetEl.style.pointerEvents = 'auto';
    }
  }
}

if (typeof module !== 'undefined') {
  module.exports = { ScreenRouter };
}

  },

  "game/path": function(module, exports, require) {
/**
 * Path and Waypoints Navigation Engine.
 */

class PathFollower {
  constructor(waypoints = []) {
    this.waypoints = waypoints;
    this.currentIndex = 0;
    this.x = waypoints.length > 0 ? waypoints[0].x : 0;
    this.y = waypoints.length > 0 ? waypoints[0].y : 0;
    this.completed = false;
    this.totalDistance = this._calculateTotalDistance();
    this.distanceTraveled = 0;
  }

  _calculateTotalDistance() {
    let total = 0;
    for (let i = 0; i < this.waypoints.length - 1; i++) {
      const p1 = this.waypoints[i];
      const p2 = this.waypoints[i + 1];
      const dx = p2.x - p1.x;
      const dy = p2.y - p1.y;
      total += Math.hypot(dx, dy);
    }
    return total;
  }

  update(speed, dt) {
    if (this.completed || this.waypoints.length < 2) return;

    let moveDistance = speed * dt;
    this.distanceTraveled += moveDistance;

    while (moveDistance > 0 && !this.completed) {
      const targetPoint = this.waypoints[this.currentIndex + 1];
      if (!targetPoint) {
        this.completed = true;
        break;
      }

      const dx = targetPoint.x - this.x;
      const dy = targetPoint.y - this.y;
      const distToTarget = Math.hypot(dx, dy);

      if (distToTarget <= moveDistance) {
        // Reached this waypoint, snap to it and consume distance
        this.x = targetPoint.x;
        this.y = targetPoint.y;
        moveDistance -= distToTarget;
        this.currentIndex += 1;

        if (this.currentIndex >= this.waypoints.length - 1) {
          this.completed = true;
          break;
        }
      } else {
        // Step partially toward target waypoint
        const ratio = moveDistance / distToTarget;
        this.x += dx * ratio;
        this.y += dy * ratio;
        moveDistance = 0;
      }
    }
  }

  getRemainingDistance() {
    return Math.max(0, this.totalDistance - this.distanceTraveled);
  }
}

if (typeof module !== 'undefined') {
  module.exports = { PathFollower };
}

  },

  "game/enemy": function(module, exports, require) {
const { PathFollower } = require('./path');
const { ENEMIES } = require('./config');

let enemyCounter = 0;

class Enemy {
  constructor(typeKey, waypoints, customProps = {}) {
    const config = ENEMIES[typeKey];
    if (!config) {
      throw new Error(`[Enemy] Unknown enemy type: ${typeKey}`);
    }

    this.id = `enemy_${++enemyCounter}`;
    this.type = typeKey;
    this.name = config.name;
    this.title = config.title || '';
    this.maxHp = config.hp;
    this.hp = config.hp;
    this.baseSpeed = config.speed;
    this.speed = config.speed;
    this.reward = config.reward;
    this.xp = config.xp;
    this.baseDamage = config.baseDamage;
    this.size = config.size;
    this.color = config.color;
    this.armor = config.armor || 0;
    this.isBoss = !!config.isBoss;

    // Boss enrage properties
    this.enrageHpRatio = config.enrageHpRatio || 0.40;
    this.enraged = false;
    this.enrageSpeedMultiplier = config.enrageSpeedMultiplier || 1.45;
    this.enrageDamageMultiplier = config.enrageDamageMultiplier || 1.50;

    // Navigation
    this.path = new PathFollower(waypoints);
    this.x = this.path.x;
    this.y = this.path.y;

    // States
    this.alive = true;
    this.reachedBase = false;

    // Status effects
    this.slowTimer = 0;
    this.slowFactor = 1.0;
    this.stunTimer = 0;

    // Visual animation ticks
    this.animPhase = Math.random() * Math.PI * 2;

    // Merge any custom overrides
    Object.assign(this, customProps);
  }

  takeDamage(amount, armorPen = 0) {
    if (!this.alive) return 0;

    // Armor reduces physical damage. Armor pen ignores percentage of armor.
    const effectiveArmor = Math.max(0, this.armor * (1 - armorPen));
    const damageReduction = effectiveArmor / (effectiveArmor + 50); // standard dim-returns armor formula
    const actualDamage = Math.max(1, Math.round(amount * (1 - damageReduction)));

    this.hp -= actualDamage;

    // Check boss enrage condition
    if (this.isBoss && !this.enraged && (this.hp / this.maxHp) <= this.enrageHpRatio) {
      this.enrage();
    }

    if (this.hp <= 0) {
      this.hp = 0;
      this.alive = false;
    }

    return actualDamage;
  }

  enrage() {
    this.enraged = true;
    this.color = '#d50000'; // Fierce red glow
    this.baseSpeed *= this.enrageSpeedMultiplier;
  }

  applySlow(factor, duration) {
    if (!this.alive) return;
    this.slowFactor = Math.min(this.slowFactor, factor);
    this.slowTimer = Math.max(this.slowTimer, duration);
  }

  applyStun(duration) {
    if (!this.alive) return;
    this.stunTimer = Math.max(this.stunTimer, duration);
  }

  update(dt) {
    if (!this.alive || this.reachedBase) return;

    this.animPhase += dt * 5;

    // Update status effect timers
    if (this.stunTimer > 0) {
      this.stunTimer -= dt;
      return; // Immobilized while stunned
    }

    let currentSpeed = this.baseSpeed;
    if (this.slowTimer > 0) {
      this.slowTimer -= dt;
      currentSpeed *= this.slowFactor;
      if (this.slowTimer <= 0) {
        this.slowFactor = 1.0;
      }
    }

    this.speed = currentSpeed;
    this.path.update(this.speed, dt);
    this.x = this.path.x;
    this.y = this.path.y;

    if (this.path.completed) {
      this.reachedBase = true;
      this.alive = false;
    }
  }

  getProgress() {
    return this.path.distanceTraveled;
  }
}

if (typeof module !== 'undefined') {
  module.exports = { Enemy };
}

  },

  "game/projectile": function(module, exports, require) {
let projectileCounter = 0;

class Projectile {
  constructor(options = {}) {
    this.id = `proj_${++projectileCounter}`;
    this.x = options.startX || 0;
    this.y = options.startY || 0;
    this.target = options.target || null;
    this.targetX = options.target ? options.target.x : (options.targetX || 0);
    this.targetY = options.target ? options.target.y : (options.targetY || 0);
    this.speed = options.speed || 350;
    this.damage = options.damage || 20;
    this.type = options.type || 'archer';
    this.armorPen = options.armorPen || 0;
    this.splashRadius = options.splashRadius || 0;
    this.finished = false;
    this.color = options.color || '#fff';
    this.radius = options.radius || 4;
    this.hitCallback = options.onHit || null;
  }

  update(dt, enemies = []) {
    if (this.finished) return [];

    // If homing (archer or magic) and target is alive, update destination
    if (this.target && this.target.alive) {
      this.targetX = this.target.x;
      this.targetY = this.target.y;
    }

    const dx = this.targetX - this.x;
    const dy = this.targetY - this.y;
    const dist = Math.hypot(dx, dy);
    const step = this.speed * dt;

    if (dist <= step || dist < 6) {
      // Impact!
      this.x = this.targetX;
      this.y = this.targetY;
      this.finished = true;
      return this._handleImpact(enemies);
    } else {
      this.x += (dx / dist) * step;
      this.y += (dy / dist) * step;
      return [];
    }
  }

  _handleImpact(enemies = []) {
    const hits = [];

    if (this.splashRadius > 0) {
      // Area of Effect damage (Cannon)
      for (const enemy of enemies) {
        if (!enemy.alive) continue;
        const d = Math.hypot(enemy.x - this.x, enemy.y - this.y);
        if (d <= this.splashRadius) {
          // Linear falloff: 100% damage at center, 50% at edge
          const falloff = 1.0 - (d / this.splashRadius) * 0.5;
          const dmg = Math.round(this.damage * falloff);
          const actual = enemy.takeDamage(dmg, this.armorPen);
          hits.push({ enemy, damage: actual, isDirect: (enemy === this.target) });
        }
      }
    } else {
      // Single target damage (Archer / Magic)
      if (this.target && this.target.alive) {
        const actual = this.target.takeDamage(this.damage, this.armorPen);
        hits.push({ enemy: this.target, damage: actual, isDirect: true });
      }
    }

    if (this.hitCallback) {
      this.hitCallback(this, hits);
    }

    return hits;
  }
}

if (typeof module !== 'undefined') {
  module.exports = { Projectile };
}

  },

  "game/tower": function(module, exports, require) {
const { TOWERS } = require('./config');
const { Projectile } = require('./projectile');

let towerCounter = 0;

class Tower {
  constructor(typeKey, spotId, x, y) {
    const config = TOWERS[typeKey];
    if (!config) {
      throw new Error(`[Tower] Unknown tower type: ${typeKey}`);
    }

    this.id = `tower_${++towerCounter}`;
    this.type = typeKey;
    this.name = config.name;
    this.spotId = spotId;
    this.x = x;
    this.y = y;
    this.config = config;

    this.level = 1;
    this._loadLevelStats();

    this.cooldown = 0;
    this.target = null;
    this.totalDamageDealt = 0;
    this.kills = 0;

    // Temporary buffs (e.g., Tiko's skill)
    this.buffTimer = 0;
    this.damageBuff = 0;
    this.speedBuff = 0;
  }

  _loadLevelStats() {
    const stats = this.config.levels[this.level - 1];
    this.cost = stats.cost;
    this.baseRange = stats.range;
    this.baseDamage = stats.damage;
    this.baseFireRate = stats.fireRate;
    this.projectileSpeed = stats.projectileSpeed;
    this.armorPen = stats.armorPen || 0;
    this.splashRadius = stats.splashRadius || 0;
  }

  canUpgrade() {
    return this.level < this.config.levels.length;
  }

  getUpgradeCost() {
    if (!this.canUpgrade()) return null;
    return this.config.levels[this.level].cost;
  }

  upgrade() {
    if (!this.canUpgrade()) return false;
    this.level += 1;
    this._loadLevelStats();
    return true;
  }

  getSellValue() {
    let invested = 0;
    for (let i = 0; i < this.level; i++) {
      invested += this.config.levels[i].cost;
    }
    return Math.floor(invested * 0.70); // 70% refund
  }

  applyBuff(damageBuff, speedBuff, duration) {
    this.damageBuff = Math.max(this.damageBuff, damageBuff);
    this.speedBuff = Math.max(this.speedBuff, speedBuff);
    this.buffTimer = Math.max(this.buffTimer, duration);
  }

  get effectiveRange() {
    return this.baseRange;
  }

  get effectiveDamage() {
    const multiplier = 1 + (this.buffTimer > 0 ? this.damageBuff : 0);
    return Math.round(this.baseDamage * multiplier);
  }

  get effectiveFireRate() {
    const multiplier = 1 + (this.buffTimer > 0 ? this.speedBuff : 0);
    return this.baseFireRate * multiplier;
  }

  findTarget(enemies = []) {
    let best = null;
    let maxProgress = -1;

    for (const enemy of enemies) {
      if (!enemy.alive) continue;
      const dist = Math.hypot(enemy.x - this.x, enemy.y - this.y);
      if (dist <= this.effectiveRange) {
        // Priority to enemy furthest along the path
        const progress = enemy.getProgress();
        if (progress > maxProgress) {
          maxProgress = progress;
          best = enemy;
        }
      }
    }

    return best;
  }

  update(dt, enemies = []) {
    if (this.buffTimer > 0) {
      this.buffTimer -= dt;
      if (this.buffTimer <= 0) {
        this.damageBuff = 0;
        this.speedBuff = 0;
      }
    }

    if (this.cooldown > 0) {
      this.cooldown -= dt;
    }

    // Refresh target
    if (!this.target || !this.target.alive || Math.hypot(this.target.x - this.x, this.target.y - this.y) > this.effectiveRange) {
      this.target = this.findTarget(enemies);
    }

    // Try to attack
    if (this.cooldown <= 0 && this.target) {
      this.cooldown = 1 / this.effectiveFireRate;
      return this._fire(this.target);
    }

    return null;
  }

  _fire(target) {
    const projColor = this.type === 'archer' ? '#81c784' : (this.type === 'magic' ? '#ba68c8' : '#ff8a65');
    const projRadius = this.type === 'cannon' ? 6 : (this.type === 'magic' ? 5 : 3);

    return new Projectile({
      startX: this.x,
      startY: this.y,
      target,
      speed: this.projectileSpeed,
      damage: this.effectiveDamage,
      type: this.type,
      armorPen: this.armorPen,
      splashRadius: this.splashRadius,
      color: projColor,
      radius: projRadius
    });
  }
}

if (typeof module !== 'undefined') {
  module.exports = { Tower };
}

  },

  "game/hero": function(module, exports, require) {
const { HEROES } = require('./config');

class BattleHero {
  constructor(heroId) {
    const config = HEROES[heroId];
    if (!config) {
      throw new Error(`[BattleHero] Unknown hero: ${heroId}`);
    }

    this.id = config.id;
    this.name = config.name;
    this.title = config.title;
    this.rarity = config.rarity;
    this.role = config.role;
    this.avatar = config.avatar;
    this.skill = { ...config.skill };
    this.stats = { ...config.stats };

    this.cooldownRemaining = 0;
    this.totalDamageDealt = 0;
    this.timesCast = 0;
  }

  update(dt) {
    if (this.cooldownRemaining > 0) {
      this.cooldownRemaining = Math.max(0, this.cooldownRemaining - dt);
    }
  }

  canCast() {
    return this.cooldownRemaining <= 0;
  }

  getCooldownRatio() {
    if (this.skill.cooldown <= 0) return 0;
    return this.cooldownRemaining / this.skill.cooldown;
  }

  cast(battle, targetCoords = null) {
    if (!this.canCast()) {
      return { success: false, reason: 'on_cooldown' };
    }

    this.cooldownRemaining = this.skill.cooldown;
    this.timesCast += 1;

    // Default target coords: center of enemy concentration or center of map
    const coords = targetCoords || this._findTargetCoords(battle);
    const result = {
      success: true,
      heroId: this.id,
      skillName: this.skill.name,
      x: coords.x,
      y: coords.y,
      affectedEnemies: 0,
      damageDealt: 0,
      details: ''
    };

    switch (this.id) {
      case 'kael': {
        // Explosão Arcana: 150 magic AoE damage in radius 180
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const d = Math.hypot(enemy.x - coords.x, enemy.y - coords.y);
          if (d <= this.skill.radius) {
            const actual = enemy.takeDamage(this.skill.damage, 0.4); // 40% magic penetration
            result.damageDealt += actual;
            result.affectedEnemies += 1;
          }
        }
        result.details = `${result.affectedEnemies} inimigos atingidos pela Explosão Arcana.`;
        break;
      }

      case 'lyra': {
        // Chuva de Flechas: 200 physical AoE damage in radius 200
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const d = Math.hypot(enemy.x - coords.x, enemy.y - coords.y);
          if (d <= this.skill.radius) {
            const actual = enemy.takeDamage(this.skill.damage, 0.1);
            result.damageDealt += actual;
            result.affectedEnemies += 1;
          }
        }
        result.details = `${result.affectedEnemies} atingidos pela Chuva de Flechas.`;
        break;
      }

      case 'brakk': {
        // Impacto Sísmico: 120 damage + 3s stun in radius 190
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const d = Math.hypot(enemy.x - coords.x, enemy.y - coords.y);
          if (d <= this.skill.radius) {
            const actual = enemy.takeDamage(this.skill.damage, 0.2);
            enemy.applyStun(this.skill.stunDuration);
            result.damageDealt += actual;
            result.affectedEnemies += 1;
          }
        }
        result.details = `${result.affectedEnemies} inimigos atordoados por 3s.`;
        break;
      }

      case 'nyra': {
        // Campo de Lentidão: 80 damage + 60% slow for 6s in radius 240
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const d = Math.hypot(enemy.x - coords.x, enemy.y - coords.y);
          if (d <= this.skill.radius) {
            const actual = enemy.takeDamage(this.skill.damage, 0.5);
            enemy.applySlow(this.skill.slowFactor, this.skill.duration);
            result.damageDealt += actual;
            result.affectedEnemies += 1;
          }
        }
        result.details = `${result.affectedEnemies} inimigos sob lentidão por 6s.`;
        break;
      }

      case 'tiko': {
        // Sobrecarga das Torres: +40% dmg, +60% speed for 7s to all towers
        for (const tower of battle.towers) {
          tower.applyBuff(this.skill.damageBuff, this.skill.speedBuff, this.skill.duration);
        }
        result.affectedEnemies = battle.towers.length;
        result.details = `${battle.towers.length} torres sobrecarregadas por 7s!`;
        break;
      }

      case 'vesper': {
        // Ruptura Dimensional: 450 damage to ALL enemies on battlefield
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const actual = enemy.takeDamage(this.skill.damage, 0.8);
          result.damageDealt += actual;
          result.affectedEnemies += 1;
        }
        result.details = `Ruptura Dimensional atingiu ${result.affectedEnemies} inimigos em todo o campo!`;
        break;
      }

      default:
        break;
    }

    this.totalDamageDealt += result.damageDealt;
    return result;
  }

  _findTargetCoords(battle) {
    // If there are alive enemies, find centroid of active enemies, else center of screen (270, 360)
    const alive = battle.enemies.filter(e => e.alive);
    if (alive.length === 0) {
      return { x: 270, y: 360 };
    }
    let sumX = 0;
    let sumY = 0;
    for (const e of alive) {
      sumX += e.x;
      sumY += e.y;
    }
    return {
      x: Math.round(sumX / alive.length),
      y: Math.round(sumY / alive.length)
    };
  }
}

if (typeof module !== 'undefined') {
  module.exports = { BattleHero };
}

  },

  "game/boss": function(module, exports, require) {
const { EventEmitter } = require('../core/events');

/**
 * Manages Boss Lifecycle and Cinematic Sequences.
 */
class BossController extends EventEmitter {
  constructor(bossEnemy) {
    super();
    this.boss = bossEnemy;
    this.name = bossEnemy.name;
    this.title = bossEnemy.title;

    // Cinematic state
    this.cinematicActive = true;
    this.cinematicTimer = 2.2; // 2.2 seconds presentation
    this.cinematicTotal = 2.2;
    this.shakeIntensity = 12.0;

    // Ground shockwave cooldown
    this.smashCooldown = 7.0;

    // Notified states
    this.enrageAnnounced = false;
    this.defeatedAnnounced = false;
  }

  update(dt, battle) {
    if (!this.boss) return;

    // Cinematic intro progression
    if (this.cinematicActive) {
      this.cinematicTimer -= dt;
      this.shakeIntensity = Math.max(0, 12.0 * (this.cinematicTimer / this.cinematicTotal));

      if (this.cinematicTimer <= 0) {
        this.cinematicActive = false;
        this.shakeIntensity = 0;
        this.emit('cinematic_ended', { boss: this.boss });
      }
      // Note: During cinematic, boss can pause movement or emergence
      return;
    }

    if (!this.boss.alive) {
      if (!this.defeatedAnnounced) {
        this.defeatedAnnounced = true;
        this.emit('boss_defeated', {
          name: this.name,
          title: this.title,
          rewardGold: this.boss.reward,
          rewardXp: this.boss.xp
        });
      }
      return;
    }

    // Check enrage transition
    if (this.boss.enraged && !this.enrageAnnounced) {
      this.enrageAnnounced = true;
      this.shakeIntensity = 8.0; // short tremor on enrage
      this.emit('boss_enraged', {
        name: this.name,
        message: `${this.name} entrou em FÚRIA ANCESTRAL!`
      });
    }

    // Periodic shockwave ground stomp
    this.smashCooldown -= dt;
    if (this.smashCooldown <= 0) {
      this.smashCooldown = 7.0;
      this.performShockwave(battle);
    }
  }

  performShockwave(battle) {
    if (!this.boss.alive) return;
    this.shakeIntensity = 6.0;

    // Damage all towers slightly or push back/slow nearest hero effect
    this.emit('boss_shockwave', {
      x: this.boss.x,
      y: this.boss.y,
      radius: 180,
      damage: 15
    });
  }

  getHealthRatio() {
    if (!this.boss || this.boss.maxHp <= 0) return 0;
    return Math.max(0, this.boss.hp / this.boss.maxHp);
  }

  isCinematicActive() {
    return this.cinematicActive;
  }
}

if (typeof module !== 'undefined') {
  module.exports = { BossController };
}

  },

  "game/particles": function(module, exports, require) {
/**
 * Particle and Floating Combat Text System.
 */

class ParticleSystem {
  constructor() {
    this.particles = [];
    this.floats = [];
  }

  addParticle(x, y, options = {}) {
    this.particles.push({
      x,
      y,
      vx: options.vx || (Math.random() - 0.5) * 80,
      vy: options.vy || (Math.random() - 0.5) * 80,
      size: options.size || 4,
      color: options.color || '#ffeb3b',
      life: options.life || 0.6,
      maxLife: options.life || 0.6,
      alpha: 1.0,
      type: options.type || 'circle'
    });
  }

  emitBurst(x, y, count = 12, options = {}) {
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count + (Math.random() * 0.4 - 0.2);
      const speed = (options.speed || 80) * (0.6 + Math.random() * 0.8);
      this.addParticle(x, y, {
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        size: options.size || (3 + Math.random() * 3),
        color: options.color || '#ffa000',
        life: options.life || (0.4 + Math.random() * 0.4),
        type: options.type || 'circle'
      });
    }
  }

  addFloatingText(x, y, text, color = '#ffffff') {
    this.floats.push({
      x,
      y,
      text,
      color,
      vy: -40,
      life: 0.9,
      maxLife: 0.9,
      alpha: 1.0
    });
  }

  update(dt) {
    // Update particles
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.life -= dt;
      if (p.life <= 0) {
        this.particles.splice(i, 1);
        continue;
      }
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.alpha = Math.max(0, p.life / p.maxLife);
    }

    // Update floating text
    for (let i = this.floats.length - 1; i >= 0; i--) {
      const f = this.floats[i];
      f.life -= dt;
      if (f.life <= 0) {
        this.floats.splice(i, 1);
        continue;
      }
      f.y += f.vy * dt;
      f.alpha = Math.max(0, f.life / f.maxLife);
    }
  }

  clear() {
    this.particles = [];
    this.floats = [];
  }
}

if (typeof module !== 'undefined') {
  module.exports = { ParticleSystem };
}

  },

  "game/battle": function(module, exports, require) {
const { EventEmitter } = require('../core/events');
const { STAGES, TOWERS } = require('./config');
const { Enemy } = require('./enemy');
const { Tower } = require('./tower');
const { BattleHero } = require('./hero');
const { BossController } = require('./boss');
const { ParticleSystem } = require('./particles');

class BattleSimulation extends EventEmitter {
  constructor(stageId, selectedHeroIds = ['kael'], options = {}) {
    super();
    this.stageConfig = STAGES.find(s => s.id === stageId) || STAGES[0];
    this.stageId = this.stageConfig.id;

    // Simulation state: 'ready', 'running', 'paused', 'victory', 'defeat'
    this.state = 'ready';
    this.timeScale = options.timeScale || 1.0;
    this.elapsedTime = 0;

    // Base & Economy
    this.maxBaseHp = this.stageConfig.baseHp;
    this.baseHp = this.maxBaseHp;
    this.gold = this.stageConfig.startingGold;
    this.totalGoldEarned = 0;
    this.totalXpEarned = 0;
    this.enemiesDefeated = 0;

    // Waypoints & Build spots
    this.waypoints = JSON.parse(JSON.stringify(this.stageConfig.waypoints));
    this.buildSpots = this.stageConfig.buildSpots.map(s => ({ ...s, tower: null }));

    // Entities
    this.enemies = [];
    this.towers = [];
    this.projectiles = [];
    this.particles = new ParticleSystem();

    // Heroes
    this.heroes = selectedHeroIds.map(id => new BattleHero(id));

    // Waves management
    this.currentWaveIndex = 0;
    this.totalWaves = this.stageConfig.waves.length;
    this.spawnQueue = [];
    this.spawnTimer = 0;
    this.waveIntermissionTimer = 2.0; // delay before first wave starts
    this.isSpawningWave = false;
    this.allWavesQueued = false;

    // Boss handling
    this.bossController = null;
    this.screenShake = 0;
  }

  start() {
    if (this.state === 'ready') {
      this.state = 'running';
      this.emit('battle_started', { stageId: this.stageId });
    }
  }

  pause() {
    if (this.state === 'running') {
      this.state = 'paused';
      this.emit('battle_paused');
    }
  }

  resume() {
    if (this.state === 'paused') {
      this.state = 'running';
      this.emit('battle_resumed');
    }
  }

  setSpeed(speed) {
    this.timeScale = speed === 2 ? 2.0 : 1.0;
    this.emit('speed_changed', this.timeScale);
  }

  isOver() {
    return this.state === 'victory' || this.state === 'defeat';
  }

  // --- Building & Upgrades ---

  buildTower(spotId, towerType) {
    if (this.state !== 'running' && this.state !== 'ready' && this.state !== 'paused') {
      return { success: false, reason: 'invalid_state' };
    }

    const spot = this.buildSpots.find(s => s.id === spotId);
    if (!spot) {
      return { success: false, reason: 'spot_not_found' };
    }
    if (spot.tower) {
      return { success: false, reason: 'spot_occupied' };
    }

    const config = TOWERS[towerType];
    if (!config) {
      return { success: false, reason: 'invalid_tower_type' };
    }

    const cost = config.levels[0].cost;
    if (this.gold < cost) {
      return { success: false, reason: 'not_enough_gold', required: cost, current: this.gold };
    }

    this.gold -= cost;
    const tower = new Tower(towerType, spotId, spot.x, spot.y);
    spot.tower = tower;
    this.towers.push(tower);

    this.particles.emitBurst(spot.x, spot.y, 8, { color: tower.config.color, speed: 60 });
    this.particles.addFloatingText(spot.x, spot.y - 15, `-${cost} Ouro`, '#ffd54f');

    this.emit('tower_built', { spotId, tower });
    this.emit('gold_changed', { gold: this.gold, delta: -cost });
    return { success: true, tower };
  }

  upgradeTower(spotId) {
    const spot = this.buildSpots.find(s => s.id === spotId);
    if (!spot || !spot.tower) {
      return { success: false, reason: 'no_tower' };
    }

    const tower = spot.tower;
    if (!tower.canUpgrade()) {
      return { success: false, reason: 'max_level_reached' };
    }

    const cost = tower.getUpgradeCost();
    if (this.gold < cost) {
      return { success: false, reason: 'not_enough_gold', required: cost, current: this.gold };
    }

    this.gold -= cost;
    tower.upgrade();

    this.particles.emitBurst(spot.x, spot.y, 16, { color: '#ffd700', speed: 90 });
    this.particles.addFloatingText(spot.x, spot.y - 15, `Nv.${tower.level}!`, '#fff');

    this.emit('tower_upgraded', { spotId, tower, newLevel: tower.level });
    this.emit('gold_changed', { gold: this.gold, delta: -cost });
    return { success: true, tower };
  }

  sellTower(spotId) {
    const spot = this.buildSpots.find(s => s.id === spotId);
    if (!spot || !spot.tower) {
      return { success: false, reason: 'no_tower' };
    }

    const tower = spot.tower;
    const refund = tower.getSellValue();
    this.gold += refund;

    this.towers = this.towers.filter(t => t !== tower);
    spot.tower = null;

    this.particles.addFloatingText(spot.x, spot.y - 15, `+${refund} Ouro`, '#81c784');
    this.emit('tower_sold', { spotId, refund });
    this.emit('gold_changed', { gold: this.gold, delta: refund });
    return { success: true, refund };
  }

  castHeroSkill(heroId, targetCoords = null) {
    const hero = this.heroes.find(h => h.id === heroId);
    if (!hero) {
      return { success: false, reason: 'hero_not_in_party' };
    }

    const result = hero.cast(this, targetCoords);
    if (result.success) {
      this.particles.emitBurst(result.x, result.y, 20, { color: '#9c27b0', speed: 120 });
      this.particles.addFloatingText(result.x, result.y - 20, `${hero.skill.name}!`, '#ba68c8');
      this.screenShake = 6.0;
      this.emit('hero_skill_cast', result);
    }
    return result;
  }

  // --- Wave Generation ---

  _prepareNextWave() {
    if (this.currentWaveIndex >= this.totalWaves) {
      this.allWavesQueued = true;
      return;
    }

    const waveData = this.stageConfig.waves[this.currentWaveIndex];
    this.currentWaveIndex += 1;
    this.spawnQueue = [];

    for (const group of waveData.enemies) {
      for (let i = 0; i < group.count; i++) {
        this.spawnQueue.push({
          type: group.type,
          delay: group.interval
        });
      }
    }

    this.isSpawningWave = true;
    this.spawnTimer = 0.5; // brief pause before first enemy of wave spawns
    this.emit('wave_started', {
      currentWave: this.currentWaveIndex,
      totalWaves: this.totalWaves
    });
  }

  // --- Main Simulation Loop ---

  tick(dt) {
    if (this.state !== 'running') return;

    let remaining = dt * this.timeScale;
    const stepSize = 0.05;

    while (remaining > 0 && this.state === 'running') {
      const step = Math.min(remaining, stepSize);
      this._subTick(step);
      remaining -= step;
      if (this.isOver()) break;
    }
  }

  _subTick(scaledDt) {
    this.elapsedTime += scaledDt;

    // Update screen shake decay
    if (this.screenShake > 0) {
      this.screenShake = Math.max(0, this.screenShake - scaledDt * 15);
    }

    // 1. Boss Controller Tick (if active)
    if (this.bossController) {
      this.bossController.update(scaledDt, this);
      if (this.bossController.isCinematicActive()) {
        this.screenShake = Math.max(this.screenShake, this.bossController.shakeIntensity);
        // During cinematic intro, pause enemy movement briefly for dramatic effect
        this.particles.update(scaledDt);
        return;
      }
    }

    // 2. Wave Management
    if (!this.isSpawningWave && !this.allWavesQueued) {
      this.waveIntermissionTimer -= scaledDt;
      if (this.waveIntermissionTimer <= 0) {
        this._prepareNextWave();
      }
    } else if (this.isSpawningWave) {
      this.spawnTimer -= scaledDt;
      if (this.spawnTimer <= 0) {
        if (this.spawnQueue.length > 0) {
          const item = this.spawnQueue.shift();
          this._spawnEnemy(item.type);
          this.spawnTimer = item.delay;
        } else {
          this.isSpawningWave = false;
          this.waveIntermissionTimer = 4.0; // rest period before next wave
        }
      }
    }

    // 3. Update Heroes
    for (const hero of this.heroes) {
      hero.update(scaledDt);
    }

    // 4. Update Towers & Fire
    for (const tower of this.towers) {
      const proj = tower.update(scaledDt, this.enemies);
      if (proj) {
        this.projectiles.push(proj);
      }
    }

    // 5. Update Projectiles
    for (let i = this.projectiles.length - 1; i >= 0; i--) {
      const p = this.projectiles[i];
      const hits = p.update(scaledDt, this.enemies);

      if (p.finished) {
        this.projectiles.splice(i, 1);
        if (hits.length > 0) {
          for (const hit of hits) {
            this.particles.addFloatingText(hit.enemy.x, hit.enemy.y - 10, `-${hit.damage}`, '#ff5252');
            this.particles.emitBurst(hit.enemy.x, hit.enemy.y, 4, { color: p.color, speed: 45 });
          }
        }
      }
    }

    // 6. Update Enemies
    for (let i = this.enemies.length - 1; i >= 0; i--) {
      const enemy = this.enemies[i];
      enemy.update(scaledDt);

      if (enemy.reachedBase) {
        // Damage base
        this.baseHp = Math.max(0, this.baseHp - enemy.baseDamage);
        this.screenShake = 8.0;
        this.particles.emitBurst(enemy.x, enemy.y, 14, { color: '#ff1744', speed: 90 });
        this.particles.addFloatingText(enemy.x, enemy.y - 20, `-${enemy.baseDamage} Base!`, '#ff1744');
        this.emit('base_damaged', { baseHp: this.baseHp, maxBaseHp: this.maxBaseHp, damage: enemy.baseDamage });

        this.enemies.splice(i, 1);

        if (this.baseHp <= 0) {
          this._triggerDefeat();
          return;
        }
      } else if (!enemy.alive) {
        // Defeated by tower / hero!
        this.enemiesDefeated += 1;
        this.gold += enemy.reward;
        this.totalGoldEarned += enemy.reward;
        this.totalXpEarned += enemy.xp;

        this.particles.emitBurst(enemy.x, enemy.y, 10, { color: '#ffd54f', speed: 70 });
        this.particles.addFloatingText(enemy.x, enemy.y - 15, `+${enemy.reward} Ouro`, '#ffd700');

        this.emit('enemy_defeated', { enemy, reward: enemy.reward, xp: enemy.xp });
        this.emit('gold_changed', { gold: this.gold, delta: enemy.reward });

        this.enemies.splice(i, 1);
      }
    }

    // 7. Update Particles & Floats
    this.particles.update(scaledDt);

    // 8. Check Victory Condition
    if (this.allWavesQueued && !this.isSpawningWave && this.enemies.length === 0 && this.baseHp > 0) {
      this._triggerVictory();
    }
  }

  _spawnEnemy(typeKey) {
    const enemy = new Enemy(typeKey, this.waypoints);
    this.enemies.push(enemy);

    if (enemy.isBoss) {
      this.bossController = new BossController(enemy);
      this.screenShake = 12.0;
      this.emit('boss_spawned', {
        name: enemy.name,
        title: enemy.title,
        bossController: this.bossController
      });
    }

    this.emit('enemy_spawned', { enemy });
    return enemy;
  }

  _triggerVictory() {
    this.state = 'victory';
    const totalGoldReward = this.totalGoldEarned + this.stageConfig.rewardGold;
    const totalXpReward = this.totalXpEarned + this.stageConfig.rewardXp;

    this.emit('victory', {
      stageId: this.stageId,
      baseHp: this.baseHp,
      maxBaseHp: this.maxBaseHp,
      enemiesDefeated: this.enemiesDefeated,
      rewardGold: totalGoldReward,
      rewardXp: totalXpReward,
      elapsedTime: Math.round(this.elapsedTime)
    });
  }

  _triggerDefeat() {
    this.state = 'defeat';
    this.emit('defeat', {
      stageId: this.stageId,
      waveReached: this.currentWaveIndex,
      totalWaves: this.totalWaves,
      enemiesDefeated: this.enemiesDefeated,
      elapsedTime: Math.round(this.elapsedTime)
    });
  }
}

if (typeof module !== 'undefined') {
  module.exports = { BattleSimulation };
}

  },

  "render/canvasRenderer": function(module, exports, require) {
/**
 * High Performance Canvas 2D Renderer for Medieval Fantasy Tower Defense.
 */

class CanvasRenderer {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.width = canvas.width || 540;
    this.height = canvas.height || 720;
    this.selectedSpot = null;
  }

  resize(w, h) {
    this.width = w;
    this.height = h;
    this.canvas.width = w;
    this.canvas.height = h;
  }

  render(battle, options = {}) {
    if (!this.ctx) return;
    const ctx = this.ctx;

    ctx.save();

    // 1. Screen Shake offset
    if (battle.screenShake > 0) {
      const sx = (Math.random() - 0.5) * battle.screenShake * 2;
      const sy = (Math.random() - 0.5) * battle.screenShake * 2;
      ctx.translate(sx, sy);
    }

    // 2. Clear & Draw Map Scenery
    this._drawBackground(battle.stageConfig.theme);
    this._drawPath(battle.waypoints, battle.stageConfig.theme);
    this._drawBase(battle.waypoints[battle.waypoints.length - 1]);

    // 3. Draw Build Spots
    this._drawBuildSpots(battle.buildSpots, options.selectedSpotId);

    // 4. Draw Towers
    for (const tower of battle.towers) {
      this._drawTower(tower, tower.spotId === options.selectedSpotId);
    }

    // 5. Draw Enemies
    for (const enemy of battle.enemies) {
      this._drawEnemy(enemy);
    }

    // 6. Draw Projectiles
    for (const proj of battle.projectiles) {
      this._drawProjectile(proj);
    }

    // 7. Draw Particles & Floating Text
    this._drawParticles(battle.particles);

    // 8. Draw Range Indicator if spot selected
    if (options.selectedSpotId) {
      const spot = battle.buildSpots.find(s => s.id === options.selectedSpotId);
      if (spot) {
        this._drawRangeIndicator(spot);
      }
    }

    // 9. Draw Boss Banner / Cinematic Overlay
    if (battle.bossController && battle.bossController.isCinematicActive()) {
      this._drawBossCinematic(battle.bossController);
    }

    ctx.restore();
  }

  _drawBackground(theme) {
    const ctx = this.ctx;
    let bgGrad = ctx.createLinearGradient(0, 0, 0, this.height);

    if (theme === 'ruins') {
      bgGrad.addColorStop(0, '#263238');
      bgGrad.addColorStop(1, '#1a1f23');
    } else if (theme === 'volcano') {
      bgGrad.addColorStop(0, '#3e2723');
      bgGrad.addColorStop(1, '#1b0000');
    } else {
      // Forest
      bgGrad.addColorStop(0, '#1b3820');
      bgGrad.addColorStop(1, '#0e2013');
    }

    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, this.width, this.height);

    // Decorative grid/grass blades
    ctx.fillStyle = 'rgba(255, 255, 255, 0.02)';
    for (let x = 0; x < this.width; x += 45) {
      ctx.fillRect(x, 0, 1, this.height);
    }
    for (let y = 0; y < this.height; y += 45) {
      ctx.fillRect(0, y, this.width, 1);
    }
  }

  _drawPath(waypoints, theme) {
    if (!waypoints || waypoints.length < 2) return;
    const ctx = this.ctx;

    // Path border / trench
    ctx.beginPath();
    ctx.moveTo(waypoints[0].x, waypoints[0].y);
    for (let i = 1; i < waypoints.length; i++) {
      ctx.lineTo(waypoints[i].x, waypoints[i].y);
    }
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.lineWidth = 44;
    ctx.strokeStyle = theme === 'ruins' ? '#37474f' : (theme === 'volcano' ? '#bf360c' : '#332314');
    ctx.stroke();

    // Inner path gravel / stone
    ctx.lineWidth = 36;
    ctx.strokeStyle = theme === 'ruins' ? '#546e7a' : (theme === 'volcano' ? '#4e342e' : '#5d4037');
    ctx.stroke();

    // Center dotted trail
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 12]);
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.stroke();
    ctx.setLineDash([]);
  }

  _drawBase(basePoint) {
    if (!basePoint) return;
    const ctx = this.ctx;

    // Glowing defensive portal / castle keep
    ctx.save();
    ctx.translate(basePoint.x, basePoint.y);

    const grad = ctx.createRadialGradient(0, 0, 5, 0, 0, 36);
    grad.addColorStop(0, '#42a5f5');
    grad.addColorStop(0.5, '#1565c0');
    grad.addColorStop(1, 'transparent');

    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.arc(0, 0, 36, 0, Math.PI * 2);
    ctx.fill();

    // Base icon
    ctx.font = '24px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('🏰', 0, 0);

    ctx.restore();
  }

  _drawBuildSpots(spots, selectedSpotId) {
    const ctx = this.ctx;
    for (const spot of spots) {
      if (spot.tower) continue; // Tower rendered separately

      ctx.save();
      ctx.translate(spot.x, spot.y);

      const isSelected = spot.id === selectedSpotId;

      // Pedestal base
      ctx.fillStyle = isSelected ? '#ffb300' : 'rgba(255, 255, 255, 0.12)';
      ctx.beginPath();
      ctx.arc(0, 0, 24, 0, Math.PI * 2);
      ctx.fill();

      // Border ring
      ctx.strokeStyle = isSelected ? '#ff6f00' : 'rgba(255, 255, 255, 0.35)';
      ctx.lineWidth = isSelected ? 3 : 2;
      ctx.setLineDash([4, 4]);
      ctx.stroke();
      ctx.setLineDash([]);

      // Hammer/plus icon
      ctx.fillStyle = isSelected ? '#212121' : 'rgba(255, 255, 255, 0.6)';
      ctx.font = '16px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('➕', 0, 0);

      ctx.restore();
    }
  }

  _drawTower(tower, isSelected) {
    const ctx = this.ctx;
    ctx.save();
    ctx.translate(tower.x, tower.y);

    // Selection ring
    if (isSelected) {
      ctx.strokeStyle = '#ffd54f';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(0, 0, 28, 0, Math.PI * 2);
      ctx.stroke();
    }

    // Overclock aura (Tiko buff)
    if (tower.buffTimer > 0) {
      ctx.strokeStyle = '#ff7043';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(0, 0, 32 + Math.sin(Date.now() / 150) * 3, 0, Math.PI * 2);
      ctx.stroke();
    }

    // Pedestal
    ctx.fillStyle = '#263238';
    ctx.beginPath();
    ctx.arc(0, 0, 24, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = tower.config.color;
    ctx.lineWidth = 2;
    ctx.stroke();

    // Tower Icon
    ctx.font = '22px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(tower.config.avatar, 0, -2);

    // Level Badge
    ctx.fillStyle = '#212121';
    ctx.beginPath();
    ctx.arc(14, 14, 9, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#ffd54f';
    ctx.font = 'bold 11px sans-serif';
    ctx.fillText(tower.level.toString(), 14, 14);

    ctx.restore();
  }

  _drawEnemy(enemy) {
    const ctx = this.ctx;
    ctx.save();
    ctx.translate(enemy.x, enemy.y);

    // Stun / Slow aura
    if (enemy.stunTimer > 0) {
      ctx.strokeStyle = '#ffee58';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(0, 0, enemy.size + 6, 0, Math.PI * 2);
      ctx.stroke();
    } else if (enemy.slowTimer > 0) {
      ctx.fillStyle = 'rgba(126, 87, 194, 0.35)';
      ctx.beginPath();
      ctx.arc(0, 0, enemy.size + 4, 0, Math.PI * 2);
      ctx.fill();
    }

    // Boss Enrage aura
    if (enemy.isBoss && enemy.enraged) {
      ctx.fillStyle = 'rgba(213, 0, 0, 0.3)';
      ctx.beginPath();
      ctx.arc(0, 0, enemy.size + 10, 0, Math.PI * 2);
      ctx.fill();
    }

    // Body
    ctx.fillStyle = enemy.color;
    ctx.beginPath();
    ctx.arc(0, 0, enemy.size, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#212121';
    ctx.lineWidth = enemy.isBoss ? 3 : 1.5;
    ctx.stroke();

    // Enemy Icon/Avatar
    ctx.font = `${Math.round(enemy.size * 1.2)}px sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const icon = enemy.isBoss ? '👹' : (enemy.type === 'golem' ? '🗿' : (enemy.type === 'orc' ? '🧌' : '👺'));
    ctx.fillText(icon, 0, 0);

    // Health Bar (for non-boss, boss has top health bar)
    if (!enemy.isBoss) {
      const barW = Math.max(28, enemy.size * 2);
      const barH = 4;
      const barY = -enemy.size - 8;
      const hpRatio = Math.max(0, enemy.hp / enemy.maxHp);

      ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
      ctx.fillRect(-barW / 2, barY, barW, barH);
      ctx.fillStyle = hpRatio > 0.5 ? '#66bb6a' : (hpRatio > 0.25 ? '#ffa726' : '#ef5350');
      ctx.fillRect(-barW / 2, barY, barW * hpRatio, barH);
    }

    ctx.restore();
  }

  _drawProjectile(proj) {
    const ctx = this.ctx;
    ctx.save();
    ctx.translate(proj.x, proj.y);

    ctx.fillStyle = proj.color;
    ctx.shadowColor = proj.color;
    ctx.shadowBlur = 6;
    ctx.beginPath();
    ctx.arc(0, 0, proj.radius, 0, Math.PI * 2);
    ctx.fill();

    ctx.restore();
  }

  _drawParticles(particles) {
    const ctx = this.ctx;

    // Particles
    for (const p of particles.particles) {
      ctx.save();
      ctx.globalAlpha = p.alpha;
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }

    // Floating combat text
    ctx.save();
    ctx.font = 'bold 13px sans-serif';
    ctx.textAlign = 'center';
    for (const f of particles.floats) {
      ctx.globalAlpha = f.alpha;
      ctx.fillStyle = f.color;
      ctx.shadowColor = '#000';
      ctx.shadowBlur = 3;
      ctx.fillText(f.text, f.x, f.y);
    }
    ctx.restore();
  }

  _drawRangeIndicator(spot) {
    const ctx = this.ctx;
    const tower = spot.tower;
    const range = tower ? tower.effectiveRange : 135;

    ctx.save();
    ctx.translate(spot.x, spot.y);
    ctx.fillStyle = 'rgba(66, 165, 245, 0.12)';
    ctx.strokeStyle = 'rgba(66, 165, 245, 0.5)';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.arc(0, 0, range, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.restore();
  }

  _drawBossCinematic(bossController) {
    const ctx = this.ctx;
    ctx.save();

    // Darken background
    ctx.fillStyle = 'rgba(0, 0, 0, 0.65)';
    ctx.fillRect(0, 0, this.width, this.height);

    // Ornate Boss Title Banner
    const cy = this.height * 0.35;
    ctx.fillStyle = 'rgba(183, 28, 28, 0.9)';
    ctx.fillRect(0, cy - 45, this.width, 90);

    ctx.strokeStyle = '#ffd700';
    ctx.lineWidth = 3;
    ctx.strokeRect(0, cy - 45, this.width, 90);

    ctx.textAlign = 'center';
    ctx.fillStyle = '#ffcdd2';
    ctx.font = '14px sans-serif';
    ctx.fillText('⚠️ CHEFE ANCESTRAL DESPERTOU ⚠️', this.width / 2, cy - 20);

    ctx.fillStyle = '#fff';
    ctx.font = 'bold 22px sans-serif';
    ctx.shadowColor = '#000';
    ctx.shadowBlur = 6;
    ctx.fillText(bossController.name, this.width / 2, cy + 6);

    ctx.fillStyle = '#ffd54f';
    ctx.font = 'italic 14px sans-serif';
    ctx.fillText(`— ${bossController.title} —`, this.width / 2, cy + 28);

    ctx.restore();
  }
}

if (typeof module !== 'undefined') {
  module.exports = { CanvasRenderer };
}

  },

  "ui/screens": function(module, exports, require) {
const { STAGES, HEROES, TOWERS, RARITY } = require('../game/config');
const { BattleSimulation } = require('../game/battle');
const { CanvasRenderer } = require('../render/canvasRenderer');

/**
 * Screen Controllers implementation for full touch and event responsiveness.
 */

class MainMenuScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;

    const bind = (id, fn) => {
      const el = document.getElementById(id);
      if (el) {
        el.onclick = (e) => {
          e.preventDefault();
          fn();
        };
      }
    };

    bind('btn-menu-play', () => router.navigate('stage_select'));
    bind('btn-menu-continue', () => {
      const profile = this.gameState.getProfile();
      const highestUnlocked = Math.max(...profile.unlockedStages, 1);
      router.navigate('battle', { stageId: highestUnlocked });
    });
    bind('btn-menu-heroes', () => router.navigate('hero_collection'));
    bind('btn-menu-shop', () => router.navigate('shop'));
    bind('btn-menu-settings', () => router.navigate('settings'));
  }

  onEnter() {
    this.updateProfileDisplay();
  }

  onExit() {}

  updateProfileDisplay() {
    if (typeof document === 'undefined') return;
    const profile = this.gameState.getProfile();
    const lvlEl = document.getElementById('menu-player-level');
    const coinsEl = document.getElementById('menu-player-coins');
    const slotsEl = document.getElementById('menu-player-slots');

    if (lvlEl) lvlEl.textContent = `Nível ${profile.level}`;
    if (coinsEl) coinsEl.textContent = `${profile.coins} Ouro`;
    if (slotsEl) slotsEl.textContent = `${profile.selectedHeroes.length}/${this.gameState.getMaxHeroSlots()} Heróis`;
  }
}

class StageSelectScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;
    const backBtn = document.getElementById('btn-stage-back');
    if (backBtn) {
      backBtn.onclick = (e) => {
        e.preventDefault();
        router.back();
      };
    }
  }

  onEnter() {
    this.renderStages();
  }

  onExit() {}

  renderStages() {
    if (typeof document === 'undefined') return;
    const container = document.getElementById('stages-list');
    if (!container) return;

    container.innerHTML = '';
    const profile = this.gameState.getProfile();

    for (const stage of STAGES) {
      const isUnlocked = profile.unlockedStages.includes(stage.id);
      const isCompleted = profile.completedStages.includes(stage.id);

      const card = document.createElement('div');
      card.className = `stage-card ${isUnlocked ? 'unlocked' : 'locked'} ${isCompleted ? 'completed' : ''}`;

      card.innerHTML = `
        <div class="stage-info">
          <div class="stage-header">
            <span class="stage-num">Fase ${stage.id}</span>
            <span class="stage-theme-tag">${stage.theme.toUpperCase()}</span>
          </div>
          <div class="stage-title">${stage.name}</div>
          <div class="stage-desc">${stage.description}</div>
          <div class="stage-meta">
            <span>🛡️ Base: ${stage.baseHp}</span>
            <span>💰 Início: ${stage.startingGold}</span>
            <span>⭐ Recompensa: +${stage.rewardGold} Ouro / +${stage.rewardXp} XP</span>
          </div>
        </div>
        <div class="stage-action">
          ${isUnlocked
            ? `<button class="btn btn-primary btn-play-stage" data-id="${stage.id}">JOGAR</button>`
            : `<button class="btn btn-disabled" disabled>🔒 BLOQUEADA</button>`}
        </div>
      `;

      if (isUnlocked) {
        const btn = card.querySelector('.btn-play-stage');
        if (btn) {
          btn.onclick = (e) => {
            e.preventDefault();
            this.router.navigate('battle', { stageId: stage.id });
          };
        }
      }

      container.appendChild(card);
    }
  }
}

class HeroCollectionScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;
    const backBtn = document.getElementById('btn-heroes-back');
    if (backBtn) {
      backBtn.onclick = (e) => {
        e.preventDefault();
        router.back();
      };
    }
  }

  onEnter() {
    this.renderHeroes();
  }

  onExit() {}

  renderHeroes() {
    if (typeof document === 'undefined') return;
    const grid = document.getElementById('heroes-grid');
    const slotsEl = document.getElementById('heroes-slots-status');
    if (!grid) return;

    grid.innerHTML = '';
    const profile = this.gameState.getProfile();
    const maxSlots = this.gameState.getMaxHeroSlots();

    if (slotsEl) {
      slotsEl.textContent = `Equipe Selecionada: ${profile.selectedHeroes.length} / ${maxSlots} Espaços Desbloqueados`;
    }

    for (const [key, hero] of Object.entries(HEROES)) {
      const isUnlocked = profile.unlockedHeroes.includes(key);
      const isSelected = profile.selectedHeroes.includes(key);
      const rarityData = RARITY[hero.rarity];

      const card = document.createElement('div');
      card.className = `hero-card ${isUnlocked ? 'unlocked' : 'locked'} ${isSelected ? 'selected' : ''}`;
      card.style.borderColor = rarityData.border;

      card.innerHTML = `
        <div class="hero-rarity" style="color: ${rarityData.color}">
          <span>${rarityData.badge}</span> ${rarityData.name} — ${hero.role}
        </div>
        <div class="hero-main">
          <div class="hero-avatar" style="background: ${hero.iconColor}22; border-color: ${hero.iconColor}">
            ${hero.avatar}
          </div>
          <div class="hero-details">
            <div class="hero-name">${hero.name}</div>
            <div class="hero-title">${hero.title}</div>
            <div class="hero-skill-info">
              <strong>${hero.skill.name}:</strong> ${hero.skill.description}
            </div>
          </div>
        </div>
        <div class="hero-stats">
          <span>❤️ Vida: ${hero.stats.hp}</span>
          <span>⚔️ Ataque: ${hero.stats.attack}</span>
          <span>🛡️ Defesa: ${hero.stats.defense}</span>
          <span>⚡ Recarga: ${hero.skill.cooldown}s</span>
        </div>
        <div class="hero-card-footer">
          ${isUnlocked
            ? (isSelected
                ? `<button class="btn btn-secondary btn-deselect-hero" data-id="${key}">DESEQUIPAR</button>`
                : `<button class="btn btn-primary btn-select-hero" data-id="${key}">EQUIPAR</button>`)
            : `<div class="hero-locked-note">🔒 Desbloqueia no Nível ${hero.unlockLevel} ou Loja</div>`}
        </div>
      `;

      if (isUnlocked) {
        const selectBtn = card.querySelector('.btn-select-hero');
        const deselectBtn = card.querySelector('.btn-deselect-hero');

        if (selectBtn) {
          selectBtn.onclick = (e) => {
            e.preventDefault();
            const res = this.gameState.selectHero(key);
            if (!res.success && res.reason === 'slots_full') {
              alert(`Limite de heróis atingido! Você tem ${res.maxSlots} espaços disponíveis.`);
            }
            this.renderHeroes();
          };
        }
        if (deselectBtn) {
          deselectBtn.onclick = (e) => {
            e.preventDefault();
            this.gameState.deselectHero(key);
            this.renderHeroes();
          };
        }
      }

      grid.appendChild(card);
    }
  }
}

class ShopScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;
    const backBtn = document.getElementById('btn-shop-back');
    if (backBtn) {
      backBtn.onclick = (e) => {
        e.preventDefault();
        router.back();
      };
    }
  }

  onEnter() {
    this.renderShop();
  }

  onExit() {}

  renderShop() {
    if (typeof document === 'undefined') return;
    const container = document.getElementById('shop-items');
    const coinsEl = document.getElementById('shop-coins-counter');
    if (!container) return;

    const profile = this.gameState.getProfile();
    if (coinsEl) {
      coinsEl.textContent = `${profile.coins} Ouro`;
    }

    container.innerHTML = '';

    for (const [key, hero] of Object.entries(HEROES)) {
      if (hero.cost === 0) continue; // Initial free hero

      const isPurchased = profile.unlockedHeroes.includes(key);
      const canAfford = profile.coins >= hero.cost;
      const rarityData = RARITY[hero.rarity];

      const itemCard = document.createElement('div');
      itemCard.className = `shop-item-card ${isPurchased ? 'purchased' : ''}`;

      itemCard.innerHTML = `
        <div class="shop-hero-avatar" style="border-color: ${rarityData.border}">
          ${hero.avatar}
        </div>
        <div class="shop-hero-info">
          <div class="shop-hero-name">${hero.name} <span style="color: ${rarityData.color}">(${rarityData.name})</span></div>
          <div class="shop-hero-skill"><strong>${hero.skill.name}:</strong> ${hero.skill.description}</div>
          <div class="shop-hero-cost">💰 Custo: <strong>${hero.cost} Ouro</strong></div>
        </div>
        <div class="shop-item-action">
          ${isPurchased
            ? `<button class="btn btn-disabled" disabled>ADQUIRIDO</button>`
            : `<button class="btn ${canAfford ? 'btn-primary' : 'btn-disabled'} btn-buy-hero" data-id="${key}" ${canAfford ? '' : 'disabled'}>
                COMPRAR
              </button>`}
        </div>
      `;

      if (!isPurchased && canAfford) {
        const buyBtn = itemCard.querySelector('.btn-buy-hero');
        if (buyBtn) {
          buyBtn.onclick = (e) => {
            e.preventDefault();
            const spent = this.gameState.spendCoins(hero.cost);
            if (spent) {
              this.gameState.unlockHero(key);
              this.renderShop();
            }
          };
        }
      }

      container.appendChild(itemCard);
    }
  }
}

class SettingsScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;

    const backBtn = document.getElementById('btn-settings-back');
    if (backBtn) {
      backBtn.onclick = (e) => {
        e.preventDefault();
        router.back();
      };
    }

    const soundToggle = document.getElementById('setting-sound');
    if (soundToggle) {
      soundToggle.onchange = (e) => {
        this.gameState.updateSettings({ soundEnabled: e.target.checked });
      };
    }

    const musicToggle = document.getElementById('setting-music');
    if (musicToggle) {
      musicToggle.onchange = (e) => {
        this.gameState.updateSettings({ musicEnabled: e.target.checked });
      };
    }

    const resetBtn = document.getElementById('btn-reset-progress');
    if (resetBtn) {
      resetBtn.onclick = (e) => {
        e.preventDefault();
        if (confirm('Tem certeza que deseja apagar todo o progresso e reiniciar?')) {
          this.gameState.resetProgress();
          alert('Progresso reiniciado com sucesso!');
          this.router.navigate('main_menu');
        }
      };
    }
  }

  onEnter() {
    if (typeof document === 'undefined') return;
    const profile = this.gameState.getProfile();
    const soundToggle = document.getElementById('setting-sound');
    const musicToggle = document.getElementById('setting-music');

    if (soundToggle) soundToggle.checked = profile.settings.soundEnabled;
    if (musicToggle) musicToggle.checked = profile.settings.musicEnabled;
  }

  onExit() {}
}

class BattleScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
    this.battle = null;
    this.renderer = null;
    this.rafId = null;
    this.lastFrameTime = 0;
    this.selectedSpotId = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;

    // HUD bindings
    const bind = (id, fn) => {
      const el = document.getElementById(id);
      if (el) {
        el.onclick = (e) => {
          e.preventDefault();
          fn();
        };
      }
    };

    bind('btn-battle-pause', () => this.togglePause());
    bind('btn-battle-speed', () => this.toggleSpeed());
    bind('btn-pause-resume', () => this.resume());
    bind('btn-pause-restart', () => this.restart());
    bind('btn-pause-quit', () => this.quit());
    bind('btn-victory-advance', () => this.onVictoryAdvance());
    bind('btn-defeat-retry', () => this.restart());
    bind('btn-defeat-quit', () => this.quit());

    // Tower build actions
    bind('btn-build-archer', () => this.buildTower('archer'));
    bind('btn-build-magic', () => this.buildTower('magic'));
    bind('btn-build-cannon', () => this.buildTower('cannon'));
    bind('btn-upgrade-tower', () => this.upgradeSelectedTower());
    bind('btn-sell-tower', () => this.sellSelectedTower());
    bind('btn-close-tower-modal', () => this.closeTowerModal());

    // Canvas click handling
    const canvas = document.getElementById('battle-canvas');
    if (canvas) {
      canvas.addEventListener('click', (e) => this.onCanvasClick(e));
      canvas.addEventListener('touchstart', (e) => {
        if (e.touches && e.touches.length > 0) {
          this.onCanvasTouch(e.touches[0]);
        }
      }, { passive: true });
    }
  }

  onEnter(params = {}) {
    const stageId = params.stageId || 1;
    const profile = this.gameState.getProfile();
    const selectedHeroes = profile.selectedHeroes.length > 0 ? profile.selectedHeroes : ['kael'];

    this.battle = new BattleSimulation(stageId, selectedHeroes, {
      timeScale: profile.settings.gameSpeed || 1
    });

    // Setup canvas renderer
    if (typeof document !== 'undefined') {
      const canvas = document.getElementById('battle-canvas');
      if (canvas) {
        this.renderer = new CanvasRenderer(canvas);
      }
      this._hideAllModals();
      this.renderHeroActionBar();
    }

    // Bind simulation events
    this._bindBattleEvents();

    this.battle.start();
    this.lastFrameTime = performance.now();
    this._startLoop();
  }

  onExit() {
    this._stopLoop();
    if (this.battle) {
      this.battle.pause();
      this.battle = null;
    }
    this.selectedSpotId = null;
  }

  _bindBattleEvents() {
    this.battle.on('victory', (data) => this.handleVictory(data));
    this.battle.on('defeat', (data) => this.handleDefeat(data));
    this.battle.on('base_damaged', () => this.updateHUD());
    this.battle.on('gold_changed', () => this.updateHUD());
    this.battle.on('wave_started', () => this.updateHUD());
  }

  _startLoop() {
    if (typeof requestAnimationFrame === 'undefined') return;

    const loop = (timestamp) => {
      if (!this.battle) return;

      const dt = (timestamp - this.lastFrameTime) / 1000;
      this.lastFrameTime = timestamp;

      this.battle.tick(dt);

      if (this.renderer) {
        this.renderer.render(this.battle, { selectedSpotId: this.selectedSpotId });
      }

      this.updateHUD();
      this.updateHeroCooldowns();

      if (!this.battle.isOver()) {
        this.rafId = requestAnimationFrame(loop);
      }
    };

    this.rafId = requestAnimationFrame(loop);
  }

  _stopLoop() {
    if (this.rafId && typeof cancelAnimationFrame !== 'undefined') {
      cancelAnimationFrame(this.rafId);
      this.rafId = null;
    }
  }

  togglePause() {
    if (!this.battle) return;
    if (this.battle.state === 'running') {
      this.battle.pause();
      this._showModal('modal-pause');
    } else if (this.battle.state === 'paused') {
      this.resume();
    }
  }

  resume() {
    if (this.battle && this.battle.state === 'paused') {
      this.battle.resume();
      this._hideModal('modal-pause');
    }
  }

  restart() {
    this._hideAllModals();
    const stageId = this.battle ? this.battle.stageId : 1;
    this.onExit();
    this.onEnter({ stageId });
  }

  quit() {
    this._hideAllModals();
    this.onExit();
    this.router.navigate('main_menu');
  }

  toggleSpeed() {
    if (!this.battle) return;
    const newSpeed = this.battle.timeScale === 1 ? 2 : 1;
    this.battle.setSpeed(newSpeed);
    const speedBtn = document.getElementById('btn-battle-speed');
    if (speedBtn) speedBtn.textContent = `${newSpeed}x`;
  }

  onCanvasClick(e) {
    if (typeof document === 'undefined') return;
    const canvas = document.getElementById('battle-canvas');
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const clickX = (e.clientX - rect.left) * scaleX;
    const clickY = (e.clientY - rect.top) * scaleY;

    this._handleFieldInteraction(clickX, clickY);
  }

  onCanvasTouch(touch) {
    if (typeof document === 'undefined') return;
    const canvas = document.getElementById('battle-canvas');
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const clickX = (touch.clientX - rect.left) * scaleX;
    const clickY = (touch.clientY - rect.top) * scaleY;

    this._handleFieldInteraction(clickX, clickY);
  }

  _handleFieldInteraction(x, y) {
    if (!this.battle) return;

    // Check click on build spots
    for (const spot of this.battle.buildSpots) {
      const dist = Math.hypot(spot.x - x, spot.y - y);
      if (dist <= 32) {
        this.selectedSpotId = spot.id;
        this._showTowerPanel(spot);
        return;
      }
    }

    // Clicked elsewhere on field -> dismiss build panel
    this.selectedSpotId = null;
    this.closeTowerModal();
  }

  _showTowerPanel(spot) {
    if (typeof document === 'undefined') return;
    const modal = document.getElementById('modal-tower-actions');
    if (!modal) return;

    const buildActions = document.getElementById('tower-build-options');
    const manageActions = document.getElementById('tower-manage-options');

    if (spot.tower) {
      // Existing tower: show upgrade and sell
      if (buildActions) buildActions.style.display = 'none';
      if (manageActions) manageActions.style.display = 'flex';

      const tower = spot.tower;
      const upgradeBtn = document.getElementById('btn-upgrade-tower');
      const sellBtn = document.getElementById('btn-sell-tower');
      const infoEl = document.getElementById('tower-manage-info');

      if (infoEl) {
        infoEl.textContent = `${tower.name} (Nível ${tower.level}) — Dano: ${tower.effectiveDamage} | Alcance: ${tower.effectiveRange}`;
      }

      if (upgradeBtn) {
        if (tower.canUpgrade()) {
          const cost = tower.getUpgradeCost();
          upgradeBtn.textContent = `Melhorar (+Nv.${tower.level + 1}) — 💰 ${cost}`;
          upgradeBtn.disabled = this.battle.gold < cost;
        } else {
          upgradeBtn.textContent = 'Nível Máximo Atingido';
          upgradeBtn.disabled = true;
        }
      }

      if (sellBtn) {
        sellBtn.textContent = `Vender (+💰 ${tower.getSellValue()})`;
      }
    } else {
      // Empty spot: show tower choices
      if (buildActions) buildActions.style.display = 'flex';
      if (manageActions) manageActions.style.display = 'none';

      // Update affordability on buttons
      const updateCostBtn = (id, type) => {
        const btn = document.getElementById(id);
        if (btn) {
          const cost = TOWERS[type].levels[0].cost;
          btn.disabled = this.battle.gold < cost;
        }
      };
      updateCostBtn('btn-build-archer', 'archer');
      updateCostBtn('btn-build-magic', 'magic');
      updateCostBtn('btn-build-cannon', 'cannon');
    }

    modal.style.display = 'flex';
  }

  closeTowerModal() {
    this.selectedSpotId = null;
    this._hideModal('modal-tower-actions');
  }

  buildTower(type) {
    if (!this.battle || !this.selectedSpotId) return;
    const res = this.battle.buildTower(this.selectedSpotId, type);
    if (res.success) {
      this.closeTowerModal();
    }
  }

  upgradeSelectedTower() {
    if (!this.battle || !this.selectedSpotId) return;
    const res = this.battle.upgradeTower(this.selectedSpotId);
    if (res.success) {
      const spot = this.battle.buildSpots.find(s => s.id === this.selectedSpotId);
      if (spot) this._showTowerPanel(spot);
    }
  }

  sellSelectedTower() {
    if (!this.battle || !this.selectedSpotId) return;
    const res = this.battle.sellTower(this.selectedSpotId);
    if (res.success) {
      this.closeTowerModal();
    }
  }

  renderHeroActionBar() {
    if (typeof document === 'undefined') return;
    const bar = document.getElementById('battle-hero-bar');
    if (!bar || !this.battle) return;

    bar.innerHTML = '';
    for (const hero of this.battle.heroes) {
      const btn = document.createElement('button');
      btn.className = 'hero-skill-btn';
      btn.id = `hero-btn-${hero.id}`;
      btn.innerHTML = `
        <div class="hero-skill-avatar">${hero.avatar}</div>
        <div class="hero-skill-name">${hero.name}</div>
        <div class="hero-cooldown-overlay" id="cd-${hero.id}"></div>
      `;

      btn.onclick = (e) => {
        e.preventDefault();
        this.battle.castHeroSkill(hero.id);
      };

      bar.appendChild(btn);
    }
  }

  updateHeroCooldowns() {
    if (typeof document === 'undefined' || !this.battle) return;
    for (const hero of this.battle.heroes) {
      const cdEl = document.getElementById(`cd-${hero.id}`);
      const btn = document.getElementById(`hero-btn-${hero.id}`);
      if (!cdEl || !btn) continue;

      const ratio = hero.getCooldownRatio();
      if (ratio > 0) {
        cdEl.style.height = `${Math.round(ratio * 100)}%`;
        btn.classList.add('on-cooldown');
      } else {
        cdEl.style.height = '0%';
        btn.classList.remove('on-cooldown');
      }
    }
  }

  updateHUD() {
    if (typeof document === 'undefined' || !this.battle) return;
    const baseHpEl = document.getElementById('hud-base-hp');
    const goldEl = document.getElementById('hud-gold');
    const waveEl = document.getElementById('hud-wave');
    const bossHud = document.getElementById('hud-boss-container');
    const bossBar = document.getElementById('hud-boss-fill');

    if (baseHpEl) {
      baseHpEl.textContent = `${this.battle.baseHp}/${this.battle.maxBaseHp}`;
    }
    if (goldEl) {
      goldEl.textContent = `${this.battle.gold}`;
    }
    if (waveEl) {
      waveEl.textContent = `Onda ${this.battle.currentWaveIndex}/${this.battle.totalWaves}`;
    }

    // Boss bar visibility
    if (bossHud && bossBar) {
      if (this.battle.bossController && this.battle.bossController.boss.alive) {
        bossHud.style.display = 'block';
        const ratio = this.battle.bossController.getHealthRatio();
        bossBar.style.width = `${Math.round(ratio * 100)}%`;
      } else {
        bossHud.style.display = 'none';
      }
    }
  }

  handleVictory(data) {
    this._stopLoop();
    this.gameState.addCoins(data.rewardGold);
    const xpRes = this.gameState.addXp(data.rewardXp);
    this.gameState.completeStage(data.stageId);

    if (typeof document !== 'undefined') {
      const goldRewardEl = document.getElementById('victory-gold-reward');
      const xpRewardEl = document.getElementById('victory-xp-reward');
      const levelUpNote = document.getElementById('victory-levelup-note');

      if (goldRewardEl) goldRewardEl.textContent = `+${data.rewardGold} Ouro`;
      if (xpRewardEl) xpRewardEl.textContent = `+${data.rewardXp} XP`;
      if (levelUpNote) {
        if (xpRes.levelUps > 0) {
          levelUpNote.textContent = `🎉 SUBIU PARA O NÍVEL ${xpRes.newLevel}! Novos heróis ou espaços desbloqueados!`;
          levelUpNote.style.display = 'block';
        } else {
          levelUpNote.style.display = 'none';
        }
      }

      this._showModal('modal-victory');
    }
  }

  handleDefeat(data) {
    this._stopLoop();
    if (typeof document !== 'undefined') {
      const summaryEl = document.getElementById('defeat-summary');
      if (summaryEl) {
        summaryEl.textContent = `Você resistiu até a Onda ${data.waveReached}/${data.totalWaves} e derrotou ${data.enemiesDefeated} inimigos.`;
      }
      this._showModal('modal-defeat');
    }
  }

  onVictoryAdvance() {
    this._hideAllModals();
    this.onExit();
    this.router.navigate('stage_select');
  }

  _showModal(id) {
    if (typeof document === 'undefined') return;
    const modal = document.getElementById(id);
    if (modal) modal.style.display = 'flex';
  }

  _hideModal(id) {
    if (typeof document === 'undefined') return;
    const modal = document.getElementById(id);
    if (modal) modal.style.display = 'none';
  }

  _hideAllModals() {
    if (typeof document === 'undefined') return;
    const modals = document.querySelectorAll('.game-modal');
    modals.forEach(m => m.style.display = 'none');
  }
}

if (typeof module !== 'undefined') {
  module.exports = {
    MainMenuScreen,
    StageSelectScreen,
    HeroCollectionScreen,
    ShopScreen,
    SettingsScreen,
    BattleScreen
  };
}

  },

  "index": function(module, exports, require) {
const { GameState } = require('./core/state');
const { ScreenRouter } = require('./core/router');
const {
  MainMenuScreen,
  StageSelectScreen,
  HeroCollectionScreen,
  ShopScreen,
  SettingsScreen,
  BattleScreen
} = require('./ui/screens');

function initializeGame() {
  const gameState = new GameState();
  const router = new ScreenRouter({ defaultRoute: 'main_menu' });

  // Register screens
  router.registerRoute('main_menu', new MainMenuScreen(gameState));
  router.registerRoute('stage_select', new StageSelectScreen(gameState));
  router.registerRoute('hero_collection', new HeroCollectionScreen(gameState));
  router.registerRoute('shop', new ShopScreen(gameState));
  router.registerRoute('settings', new SettingsScreen(gameState));
  router.registerRoute('battle', new BattleScreen(gameState));

  // Boot up into main menu
  router.navigate('main_menu');

  const instance = { gameState, router };

  if (typeof window !== 'undefined') {
    window.game = instance;
  }

  return instance;
}

if (typeof window !== 'undefined') {
  window.addEventListener('DOMContentLoaded', () => {
    initializeGame();
  });
}

if (typeof module !== 'undefined') {
  module.exports = { initializeGame };
}

  }
  };
  const cache = {};

  function normalizePath(base, rel) {
    if (!rel.startsWith('.')) return rel;
    const baseParts = base.split('/').slice(0, -1);
    const relParts = rel.split('/');
    for (const p of relParts) {
      if (p === '.') continue;
      if (p === '..') {
        baseParts.pop();
      } else {
        baseParts.push(p);
      }
    }
    return baseParts.join('/').replace(/\.js$/, '');
  }

  function makeRequire(currentModuleId) {
    return function(id) {
      const resolved = normalizePath(currentModuleId, id);
      if (cache[resolved]) {
        return cache[resolved].exports;
      }
      if (!modules[resolved]) {
        throw new Error("[Bundle Runtime] Cannot find module '" + id + "' (resolved as '" + resolved + "') from '" + currentModuleId + "'");
      }
      const mod = { exports: {} };
      cache[resolved] = mod;
      modules[resolved](mod, mod.exports, makeRequire(resolved));
      return mod.exports;
    };
  }

  // Bootstrap entry point
  const rootRequire = makeRequire('root');
  rootRequire('index');
})();
