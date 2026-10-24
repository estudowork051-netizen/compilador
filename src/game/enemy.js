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
