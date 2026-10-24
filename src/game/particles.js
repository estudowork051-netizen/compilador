/**
 * Particle and Floating Combat Text System.
 */

class ParticleSystem {
  constructor() {
    this.particles = [];
    this.floats = [];
  }

  addParticle(x, y, options = {}) {
    this.particles.push({
      x,
      y,
      vx: options.vx || (Math.random() - 0.5) * 80,
      vy: options.vy || (Math.random() - 0.5) * 80,
      size: options.size || 4,
      color: options.color || '#ffeb3b',
      life: options.life || 0.6,
      maxLife: options.life || 0.6,
      alpha: 1.0,
      type: options.type || 'circle'
    });
  }

  emitBurst(x, y, count = 12, options = {}) {
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count + (Math.random() * 0.4 - 0.2);
      const speed = (options.speed || 80) * (0.6 + Math.random() * 0.8);
      this.addParticle(x, y, {
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        size: options.size || (3 + Math.random() * 3),
        color: options.color || '#ffa000',
        life: options.life || (0.4 + Math.random() * 0.4),
        type: options.type || 'circle'
      });
    }
  }

  addFloatingText(x, y, text, color = '#ffffff') {
    this.floats.push({
      x,
      y,
      text,
      color,
      vy: -40,
      life: 0.9,
      maxLife: 0.9,
      alpha: 1.0
    });
  }

  update(dt) {
    // Update particles
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.life -= dt;
      if (p.life <= 0) {
        this.particles.splice(i, 1);
        continue;
      }
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.alpha = Math.max(0, p.life / p.maxLife);
    }

    // Update floating text
    for (let i = this.floats.length - 1; i >= 0; i--) {
      const f = this.floats[i];
      f.life -= dt;
      if (f.life <= 0) {
        this.floats.splice(i, 1);
        continue;
      }
      f.y += f.vy * dt;
      f.alpha = Math.max(0, f.life / f.maxLife);
    }
  }

  clear() {
    this.particles = [];
    this.floats = [];
  }
}

if (typeof module !== 'undefined') {
  module.exports = { ParticleSystem };
}
