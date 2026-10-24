const test = require('node:test');
const assert = require('node:assert');
const { BattleSimulation } = require('../src/game/battle');

test('Boss — Etapa 7: Spawning, Cinematic Sequence and Emergence', () => {
  // Stage 2 is the Ancient Ruins stage containing the boss
  const battle = new BattleSimulation(2, ['kael']);
  battle.start();

  let bossSpawnedEvent = false;
  battle.on('boss_spawned', (data) => {
    bossSpawnedEvent = true;
    assert.strictEqual(data.name, 'Ogro das Ruínas');
    assert.strictEqual(data.title, 'Devastador Ancestral');
  });

  const boss = battle._spawnEnemy('ogre_boss');
  assert.strictEqual(bossSpawnedEvent, true);
  assert.ok(battle.bossController !== null, 'Boss controller must be instantiated');
  assert.strictEqual(battle.bossController.isCinematicActive(), true, 'Cinematic sequence must begin active');
  assert.ok(battle.screenShake > 0, 'Camera tremor must accompany boss entrance');

  // Advance time past cinematic duration (2.2s)
  battle.tick(2.3);
  assert.strictEqual(battle.bossController.isCinematicActive(), false, 'Cinematic sequence should complete smoothly');
});

test('Boss — Etapa 7: Enraged Phase below 40% HP and Defeat Rewards', () => {
  const battle = new BattleSimulation(2, ['kael']);
  battle.start();

  const boss = battle._spawnEnemy('ogre_boss');
  // Complete cinematic
  battle.tick(2.5);

  const initialSpeed = boss.baseSpeed;
  assert.strictEqual(boss.enraged, false);

  // Deal true damage to drop HP below 40% (maxHp is 1700, 40% is 680)
  boss.takeDamage(1100, 1.0); // Leaves 600 HP (<40%)

  assert.strictEqual(boss.enraged, true, 'Boss must enter enraged phase');
  assert.ok(boss.baseSpeed > initialSpeed, 'Enraged boss must move faster');

  // Deliver final fatal blow
  let bossDefeatedEvent = false;
  battle.bossController.on('boss_defeated', (data) => {
    bossDefeatedEvent = true;
    assert.strictEqual(data.rewardGold, 350);
    assert.strictEqual(data.rewardXp, 350);
  });

  boss.takeDamage(1000);
  assert.strictEqual(boss.alive, false);
  battle.tick(0.1);

  assert.strictEqual(bossDefeatedEvent, true, 'Boss defeat event must fire with rewards');
});
