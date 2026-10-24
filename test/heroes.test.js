const test = require('node:test');
const assert = require('node:assert');
const { BattleSimulation } = require('../src/game/battle');
const { HEROES, RARITY } = require('../src/game/config');

test('Heroes — Etapa 4: Registry, Roles and Rarities', () => {
  const expectedHeroes = ['kael', 'lyra', 'brakk', 'nyra', 'tiko', 'vesper'];

  for (const id of expectedHeroes) {
    const h = HEROES[id];
    assert.ok(h, `Hero ${id} must exist in registry`);
    assert.ok(h.name, `Hero ${id} must have a name`);
    assert.ok(h.title, `Hero ${id} must have a title`);
    assert.ok(h.skill, `Hero ${id} must have a defined skill`);
    assert.ok(h.skill.cooldown > 0, `Skill must have real cooldown`);
    assert.ok(RARITY[h.rarity], `Rarity ${h.rarity} must be valid`);
  }
});

test('Heroes — Etapa 4: Kael (Explosão Arcana) AoE Combat Skill', () => {
  const battle = new BattleSimulation(1, ['kael']);
  battle.start();

  const enemy = battle._spawnEnemy('goblin');
  enemy.x = 270;
  enemy.y = 360;
  const initialHp = enemy.hp;

  const result = battle.castHeroSkill('kael', { x: 270, y: 360 });
  assert.strictEqual(result.success, true);
  assert.ok(enemy.hp < initialHp, 'Enemy must take damage from Explosão Arcana');

  // Cooldown check
  const castAgain = battle.castHeroSkill('kael');
  assert.strictEqual(castAgain.success, false, 'Should be prevented by cooldown');
});

test('Heroes — Etapa 4: Lyra (Chuva de Flechas) Skill', () => {
  const battle = new BattleSimulation(1, ['lyra']);
  battle.start();

  const enemy = battle._spawnEnemy('orc');
  enemy.x = 200;
  enemy.y = 200;
  const initialHp = enemy.hp;

  const result = battle.castHeroSkill('lyra', { x: 200, y: 200 });
  assert.strictEqual(result.success, true);
  assert.ok(enemy.hp < initialHp);
});

test('Heroes — Etapa 4: Brakk (Impacto Sísmico) Stun Effect', () => {
  const battle = new BattleSimulation(1, ['brakk']);
  battle.start();

  const enemy = battle._spawnEnemy('golem'); // Golem has 380 HP, survives 120 damage
  enemy.x = 250;
  enemy.y = 250;
  const initialHp = enemy.hp;

  const result = battle.castHeroSkill('brakk', { x: 250, y: 250 });
  assert.strictEqual(result.success, true);
  assert.ok(enemy.hp < initialHp, 'Enemy must take damage');
  assert.ok(enemy.stunTimer > 0, 'Enemy must be stunned');

  // Stunned enemy should not move on tick
  const posX = enemy.x;
  const posY = enemy.y;
  battle.tick(0.5);
  assert.strictEqual(enemy.x, posX, 'Stunned enemy cannot move');
  assert.strictEqual(enemy.y, posY, 'Stunned enemy cannot move');
});

test('Heroes — Etapa 4: Nyra (Campo de Lentidão) Slow Effect', () => {
  const battle = new BattleSimulation(1, ['nyra']);
  battle.start();

  const enemy = battle._spawnEnemy('orc'); // Orc has 160 HP, survives 80 damage
  enemy.x = 250;
  enemy.y = 250;
  const initialHp = enemy.hp;

  const result = battle.castHeroSkill('nyra', { x: 250, y: 250 });
  assert.strictEqual(result.success, true);
  assert.ok(enemy.hp < initialHp, 'Enemy must take damage');
  assert.ok(enemy.slowTimer > 0, 'Enemy must receive slow debuff');
  assert.strictEqual(enemy.slowFactor, 0.40, 'Speed must be reduced to 40%');
});

test('Heroes — Etapa 4: Tiko (Sobrecarga das Torres) Buff', () => {
  const battle = new BattleSimulation(1, ['tiko']);
  battle.start();

  const spot = battle.buildSpots[0];
  battle.buildTower(spot.id, 'archer');
  const tower = spot.tower;

  const baseDmg = tower.baseDamage;
  const baseFireRate = tower.baseFireRate;

  const result = battle.castHeroSkill('tiko');
  assert.strictEqual(result.success, true);
  assert.ok(tower.buffTimer > 0, 'Tower must have buff timer active');
  assert.ok(tower.effectiveDamage > baseDmg, 'Damage must be increased by overcharge');
  assert.ok(tower.effectiveFireRate > baseFireRate, 'Fire rate must be boosted by overcharge');
});

test('Heroes — Etapa 4: Vesper (Ruptura Dimensional) Battlefield-Wide Cleave', () => {
  const battle = new BattleSimulation(1, ['vesper']);
  battle.start();

  const e1 = battle._spawnEnemy('goblin');
  e1.x = 50; e1.y = 50;
  const e2 = battle._spawnEnemy('orc');
  e2.x = 450; e2.y = 600;

  const hp1 = e1.hp;
  const hp2 = e2.hp;

  const result = battle.castHeroSkill('vesper');
  assert.strictEqual(result.success, true);
  assert.ok(e1.hp < hp1, 'Enemy 1 must take massive damage anywhere on map');
  assert.ok(e2.hp < hp2, 'Enemy 2 must take massive damage anywhere on map');
});
