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
