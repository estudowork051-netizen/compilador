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
