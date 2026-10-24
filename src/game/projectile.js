let projectileCounter = 0;

class Projectile {
  constructor(options = {}) {
    this.id = `proj_${++projectileCounter}`;
    this.x = options.startX || 0;
    this.y = options.startY || 0;
    this.target = options.target || null;
    this.targetX = options.target ? options.target.x : (options.targetX || 0);
    this.targetY = options.target ? options.target.y : (options.targetY || 0);
    this.speed = options.speed || 350;
    this.damage = options.damage || 20;
    this.type = options.type || 'archer';
    this.armorPen = options.armorPen || 0;
    this.splashRadius = options.splashRadius || 0;
    this.finished = false;
    this.color = options.color || '#fff';
    this.radius = options.radius || 4;
    this.hitCallback = options.onHit || null;
  }

  update(dt, enemies = []) {
    if (this.finished) return [];

    // If homing (archer or magic) and target is alive, update destination
    if (this.target && this.target.alive) {
      this.targetX = this.target.x;
      this.targetY = this.target.y;
    }

    const dx = this.targetX - this.x;
    const dy = this.targetY - this.y;
    const dist = Math.hypot(dx, dy);
    const step = this.speed * dt;

    if (dist <= step || dist < 6) {
      // Impact!
      this.x = this.targetX;
      this.y = this.targetY;
      this.finished = true;
      return this._handleImpact(enemies);
    } else {
      this.x += (dx / dist) * step;
      this.y += (dy / dist) * step;
      return [];
    }
  }

  _handleImpact(enemies = []) {
    const hits = [];

    if (this.splashRadius > 0) {
      // Area of Effect damage (Cannon)
      for (const enemy of enemies) {
        if (!enemy.alive) continue;
        const d = Math.hypot(enemy.x - this.x, enemy.y - this.y);
        if (d <= this.splashRadius) {
          // Linear falloff: 100% damage at center, 50% at edge
          const falloff = 1.0 - (d / this.splashRadius) * 0.5;
          const dmg = Math.round(this.damage * falloff);
          const actual = enemy.takeDamage(dmg, this.armorPen);
          hits.push({ enemy, damage: actual, isDirect: (enemy === this.target) });
        }
      }
    } else {
      // Single target damage (Archer / Magic)
      if (this.target && this.target.alive) {
        const actual = this.target.takeDamage(this.damage, this.armorPen);
        hits.push({ enemy: this.target, damage: actual, isDirect: true });
      }
    }

    if (this.hitCallback) {
      this.hitCallback(this, hits);
    }

    return hits;
  }
}

if (typeof module !== 'undefined') {
  module.exports = { Projectile };
}
