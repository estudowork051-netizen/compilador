const test = require('node:test');
const assert = require('node:assert');
const { GameState } = require('../src/core/state');
const { StorageManager } = require('../src/core/storage');

test('Progression — Etapa 5: Level Milestones, Hero Unlocks and Slot Expansion', () => {
  const storage = new StorageManager('test_progression_key');
  storage.clear();
  const gameState = new GameState(storage);

  const profile = gameState.getProfile();
  assert.strictEqual(profile.level, 1, 'Player starts at level 1');
  assert.strictEqual(gameState.getMaxHeroSlots(), 1, 'Level 1 has 1 hero slot');
  assert.deepStrictEqual(profile.unlockedHeroes, ['kael'], 'Level 1 starts with Kael');

  // Reach Level 3 (XP required: 350)
  gameState.addXp(350);
  assert.strictEqual(gameState.getProfile().level, 3, 'Player should reach level 3');
  assert.strictEqual(gameState.getMaxHeroSlots(), 2, 'Level 3 unlocks 2 hero slots');
  assert.ok(gameState.getProfile().unlockedHeroes.includes('lyra'), 'Level 3 must unlock Lyra');

  // Reach Level 5 (XP required: 1050)
  gameState.addXp(700);
  assert.strictEqual(gameState.getProfile().level, 5, 'Player should reach level 5');
  assert.strictEqual(gameState.getMaxHeroSlots(), 3, 'Level 5 unlocks 3 hero slots');
  assert.ok(gameState.getProfile().unlockedHeroes.includes('brakk'), 'Level 5 must unlock Brakk');

  // Reach Level 7 (XP required: 2150)
  gameState.addXp(1100);
  assert.strictEqual(gameState.getProfile().level, 7, 'Player should reach level 7');
  assert.strictEqual(gameState.getMaxHeroSlots(), 4, 'Level 7 unlocks 4 hero slots');
  assert.ok(gameState.getProfile().unlockedHeroes.includes('nyra'), 'Level 7 must unlock Nyra');

  // Reach Level 9 (XP required: 3650)
  gameState.addXp(1500);
  assert.strictEqual(gameState.getProfile().level, 9, 'Player should reach level 9');
  assert.strictEqual(gameState.getMaxHeroSlots(), 5, 'Level 9 unlocks 5 hero slots');
  assert.ok(gameState.getProfile().unlockedHeroes.includes('tiko'), 'Level 9 must unlock Tiko');

  storage.clear();
});

test('Progression — Etapa 5: Intermediate Levels Preserve Slots and Grant Gold', () => {
  const storage = new StorageManager('test_intermediate_key');
  storage.clear();
  const gameState = new GameState(storage);

  // Level 1 -> Level 2 (150 XP)
  const coinsBefore = gameState.getProfile().coins;
  gameState.addXp(150);

  assert.strictEqual(gameState.getProfile().level, 2);
  assert.strictEqual(gameState.getMaxHeroSlots(), 1, 'Slots preserved');
  assert.strictEqual(gameState.getProfile().coins, coinsBefore + 100, 'Gold reward granted');

  storage.clear();
});
