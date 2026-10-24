const test = require('node:test');
const assert = require('node:assert');
const { BattleSimulation } = require('../src/game/battle');

test('Towers — Etapa 3: Tower Placement, Cost Deduction and Validation', () => {
  const battle = new BattleSimulation(1, ['kael']);
  battle.start();

  const spot1 = battle.buildSpots[0];
  const initialGold = battle.gold;

  // Build Archer tower (cost 100)
  const res1 = battle.buildTower(spot1.id, 'archer');
  assert.strictEqual(res1.success, true);
  assert.strictEqual(battle.towers.length, 1);
  assert.strictEqual(battle.gold, initialGold - 100);
  assert.ok(spot1.tower !== null);

  // Attempting to build on already occupied spot must fail
  const resOccupied = battle.buildTower(spot1.id, 'magic');
  assert.strictEqual(resOccupied.success, false);
  assert.strictEqual(resOccupied.reason, 'spot_occupied');

  // Attempting to build with insufficient gold
  battle.gold = 10;
  const spot2 = battle.buildSpots[1];
  const resBroke = battle.buildTower(spot2.id, 'archer');
  assert.strictEqual(resBroke.success, false);
  assert.strictEqual(resBroke.reason, 'not_enough_gold');
});

test('Towers — Etapa 3: Targeting and Direct Attack Damage', () => {
  const battle = new BattleSimulation(1, ['kael']);
  battle.start();

  const spot = battle.buildSpots[0];
  battle.buildTower(spot.id, 'archer');
  const tower = spot.tower;

  // Spawn an enemy directly within tower range
  const enemy = battle._spawnEnemy('goblin');
  enemy.path.x = tower.x + 20;
  enemy.path.y = tower.y + 20;
  enemy.x = enemy.path.x;
  enemy.y = enemy.path.y;

  const initialHp = enemy.hp;

  // Tick simulation to fire and hit
  battle.tick(0.2);
  assert.ok(enemy.hp < initialHp, 'Enemy must receive damage from projectile hit');
});

test('Towers — Etapa 3: Cannon AoE Splash Damage', () => {
  const battle = new BattleSimulation(1, ['kael']);
  battle.start();

  const spot = battle.buildSpots[0];
  battle.gold = 500;
  battle.buildTower(spot.id, 'cannon');
  const tower = spot.tower;

  // Spawn primary target and secondary neighboring enemy within splash radius (65)
  const enemy1 = battle._spawnEnemy('goblin');
  enemy1.path.x = tower.x + 30;
  enemy1.path.y = tower.y + 30;
  enemy1.x = enemy1.path.x;
  enemy1.y = enemy1.path.y;

  const enemy2 = battle._spawnEnemy('goblin');
  enemy2.path.x = enemy1.x + 20;
  enemy2.path.y = enemy1.y + 20;
  enemy2.x = enemy2.path.x;
  enemy2.y = enemy2.path.y;

  const hp1 = enemy1.hp;
  const hp2 = enemy2.hp;

  // Fire cannon projectile
  battle.tick(0.1);
  assert.strictEqual(battle.projectiles.length, 1);

  // Impact and splash damage
  battle.tick(0.8);
  assert.ok(enemy1.hp < hp1, 'Primary target must take damage');
  assert.ok(enemy2.hp < hp2, 'Secondary enemy within splash radius must also take AoE damage');
});

test('Towers — Etapa 3: Upgrades and Selling', () => {
  const battle = new BattleSimulation(1, ['kael']);
  battle.start();
  battle.gold = 1000;

  const spot = battle.buildSpots[0];
  battle.buildTower(spot.id, 'archer'); // Cost 100, Level 1
  const tower = spot.tower;

  const dmgLv1 = tower.effectiveDamage;
  const rangeLv1 = tower.effectiveRange;

  // Upgrade to Level 2 (Cost 120)
  const resUp1 = battle.upgradeTower(spot.id);
  assert.strictEqual(resUp1.success, true);
  assert.strictEqual(tower.level, 2);
  assert.ok(tower.effectiveDamage > dmgLv1, 'Damage should increase on level 2');
  assert.ok(tower.effectiveRange > rangeLv1, 'Range should increase on level 2');

  // Upgrade to Level 3 (Cost 180)
  const resUp2 = battle.upgradeTower(spot.id);
  assert.strictEqual(resUp2.success, true);
  assert.strictEqual(tower.level, 3);

  // Attempting to upgrade beyond level 3 must fail
  const resMax = battle.upgradeTower(spot.id);
  assert.strictEqual(resMax.success, false);
  assert.strictEqual(resMax.reason, 'max_level_reached');

  // Sell tower
  const goldBeforeSell = battle.gold;
  const resSell = battle.sellTower(spot.id);
  assert.strictEqual(resSell.success, true);
  assert.ok(resSell.refund > 0);
  assert.strictEqual(battle.gold, goldBeforeSell + resSell.refund);
  assert.strictEqual(spot.tower, null);
  assert.strictEqual(battle.towers.length, 0);
});
