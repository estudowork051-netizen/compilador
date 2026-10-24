const test = require('node:test');
const assert = require('node:assert');
const { GameState } = require('../src/core/state');
const { StorageManager, DEFAULT_PROFILE } = require('../src/core/storage');

test('Storage — Etapa 6: Persistence, Schema Migration and Corruption Recovery', () => {
  const storage = new StorageManager('test_storage_key');
  storage.clear();

  // Test default creation
  const fresh = storage.load();
  assert.strictEqual(fresh.level, 1);
  assert.strictEqual(fresh.coins, 300);

  // Modify and save
  fresh.coins = 950;
  fresh.level = 4;
  fresh.unlockedHeroes = ['kael', 'lyra'];
  storage.save(fresh);

  // Re-read from storage
  const loaded = storage.load();
  assert.strictEqual(loaded.coins, 950);
  assert.strictEqual(loaded.level, 4);
  assert.deepStrictEqual(loaded.unlockedHeroes, ['kael', 'lyra']);

  // Simulate corrupted JSON in memory
  storage._memoryFallback = '{ "corrupted": [ invalid json';
  const recovered = storage.load();
  assert.strictEqual(recovered.level, 1, 'Corrupted save should safely fall back to default profile');
  assert.strictEqual(recovered.coins, DEFAULT_PROFILE.coins);

  storage.clear();
});

test('Shop — Etapa 6: Currency Deduction, Hero Purchase and Duplicate Prevention', () => {
  const storage = new StorageManager('test_shop_key');
  storage.clear();
  const gameState = new GameState(storage);

  // Start with 300 coins
  assert.strictEqual(gameState.getProfile().coins, 300);

  // Try to buy Nyra (cost 650) with insufficient coins
  const buyExpensive = gameState.spendCoins(650);
  assert.strictEqual(buyExpensive, false, 'Should reject purchase with insufficient coins');
  assert.strictEqual(gameState.getProfile().coins, 300, 'Coins should not be deducted');

  // Buy Lyra (cost 250)
  const buyLyra = gameState.spendCoins(250);
  assert.strictEqual(buyLyra, true, 'Should allow purchase when player has enough coins');
  assert.strictEqual(gameState.getProfile().coins, 50, 'Coins deducted accurately');

  const unlockRes = gameState.unlockHero('lyra');
  assert.strictEqual(unlockRes.success, true);
  assert.ok(gameState.getProfile().unlockedHeroes.includes('lyra'));

  // Duplicate unlock attempt must return false
  const dupRes = gameState.unlockHero('lyra');
  assert.strictEqual(dupRes.success, false);
  assert.strictEqual(dupRes.reason, 'already_unlocked');

  storage.clear();
});

test('Party Roster — Etapa 6: Slot Constraints and Safe Hero Selection', () => {
  const storage = new StorageManager('test_roster_key');
  storage.clear();
  const gameState = new GameState(storage);

  // Player level 1 only has 1 hero slot
  assert.strictEqual(gameState.getMaxHeroSlots(), 1);

  // Manually unlock lyra for testing
  gameState.profile.unlockedHeroes.push('lyra');

  // Attempting to select lyra while 1/1 slot is occupied by kael
  const resFull = gameState.selectHero('lyra');
  assert.strictEqual(resFull.success, false);
  assert.strictEqual(resFull.reason, 'slots_full');

  // Level up to level 3 (unlocks 2nd slot)
  gameState.addXp(350);
  assert.strictEqual(gameState.getMaxHeroSlots(), 2);

  // Now selection should succeed
  const resSelect = gameState.selectHero('lyra');
  assert.strictEqual(resSelect.success, true);
  assert.strictEqual(gameState.getProfile().selectedHeroes.length, 2);

  // Deselecting one hero
  const resDeselect = gameState.deselectHero('lyra');
  assert.strictEqual(resDeselect.success, true);
  assert.strictEqual(gameState.getProfile().selectedHeroes.length, 1);

  // Cannot deselect the last remaining hero in party
  const resEmpty = gameState.deselectHero('kael');
  assert.strictEqual(resEmpty.success, false);
  assert.strictEqual(resEmpty.reason, 'cannot_empty_party');

  storage.clear();
});
