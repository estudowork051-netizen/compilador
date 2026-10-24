const test = require('node:test');
const assert = require('node:assert');
const { BattleSimulation } = require('../src/game/battle');

test('Battle Simulation — Etapa 2: Initialization, Spawning and Path Movement', () => {
  const battle = new BattleSimulation(1, ['kael']);
  assert.strictEqual(battle.state, 'ready');
  assert.strictEqual(battle.baseHp, 20);
  assert.strictEqual(battle.gold, 250);

  battle.start();
  assert.strictEqual(battle.state, 'running');

  // Advance time past wave intermission and first spawn delay (2.5s)
  battle.tick(2.6);

  assert.ok(battle.enemies.length > 0, 'First enemy should have spawned');
  const enemy = battle.enemies[0];
  const initialX = enemy.x;
  const initialY = enemy.y;

  // Advance time to allow movement
  battle.tick(1.0);
  assert.ok(enemy.getProgress() > 0, 'Enemy should have advanced along waypoints');
  assert.notStrictEqual(enemy.x + enemy.y, initialX + initialY, 'Coordinates should have changed');
});

test('Battle Simulation — Etapa 2: Base Damage and Defeat Condition', () => {
  const battle = new BattleSimulation(1, ['kael']);
  battle.start();

  let defeatTriggered = false;
  battle.on('defeat', () => {
    defeatTriggered = true;
  });

  // Manually force an enemy directly next to base
  const lastWaypoint = battle.waypoints[battle.waypoints.length - 1];
  const enemy = battle._spawnEnemy('goblin');
  // Teleport right before destination
  enemy.path.x = lastWaypoint.x;
  enemy.path.y = lastWaypoint.y - 2;
  enemy.path.currentIndex = battle.waypoints.length - 2;

  // Simulate step to reach base
  battle.tick(0.2);

  assert.ok(battle.baseHp < battle.maxBaseHp, 'Base HP should decrease when enemy reaches base');

  // Inflict enough damage to reduce base HP to 0
  battle.baseHp = 1;
  const finishingEnemy = battle._spawnEnemy('goblin');
  finishingEnemy.path.x = lastWaypoint.x;
  finishingEnemy.path.y = lastWaypoint.y - 2;
  finishingEnemy.path.currentIndex = battle.waypoints.length - 2;

  battle.tick(0.2);

  assert.strictEqual(battle.baseHp, 0, 'Base HP should reach 0');
  assert.strictEqual(battle.state, 'defeat', 'Battle state must transition to defeat');
  assert.strictEqual(defeatTriggered, true, 'Defeat event must fire');
});

test('Battle Simulation — Etapa 2: Victory Condition when All Waves Cleared', () => {
  const battle = new BattleSimulation(1, ['kael']);
  battle.start();

  let victoryTriggered = false;
  battle.on('victory', (data) => {
    victoryTriggered = true;
    assert.ok(data.rewardGold > 0, 'Victory must yield real gold reward');
    assert.ok(data.rewardXp > 0, 'Victory must yield real XP reward');
  });

  // Fast forward all waves and simulate wiping all enemies
  battle.currentWaveIndex = battle.totalWaves;
  battle.allWavesQueued = true;
  battle.isSpawningWave = false;
  battle.enemies = []; // All enemies cleared

  battle.tick(0.1);

  assert.strictEqual(battle.state, 'victory', 'Battle state must transition to victory');
  assert.strictEqual(victoryTriggered, true, 'Victory event must fire');
});

test('Battle Simulation — Etapa 2: Pause, Resume and Restart', () => {
  const battle = new BattleSimulation(1, ['kael']);
  battle.start();

  battle.pause();
  assert.strictEqual(battle.state, 'paused');

  const elapsedBefore = battle.elapsedTime;
  battle.tick(1.0);
  assert.strictEqual(battle.elapsedTime, elapsedBefore, 'Tick should not advance simulation while paused');

  battle.resume();
  assert.strictEqual(battle.state, 'running');
  battle.tick(1.0);
  assert.ok(battle.elapsedTime > elapsedBefore, 'Tick should advance simulation when resumed');
});
