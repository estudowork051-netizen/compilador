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
