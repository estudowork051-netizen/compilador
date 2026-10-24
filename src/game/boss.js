const { EventEmitter } = require('../core/events');

/**
 * Manages Boss Lifecycle and Cinematic Sequences.
 */
class BossController extends EventEmitter {
  constructor(bossEnemy) {
    super();
    this.boss = bossEnemy;
    this.name = bossEnemy.name;
    this.title = bossEnemy.title;

    // Cinematic state
    this.cinematicActive = true;
    this.cinematicTimer = 2.2; // 2.2 seconds presentation
    this.cinematicTotal = 2.2;
    this.shakeIntensity = 12.0;

    // Ground shockwave cooldown
    this.smashCooldown = 7.0;

    // Notified states
    this.enrageAnnounced = false;
    this.defeatedAnnounced = false;
  }

  update(dt, battle) {
    if (!this.boss) return;

    // Cinematic intro progression
    if (this.cinematicActive) {
      this.cinematicTimer -= dt;
      this.shakeIntensity = Math.max(0, 12.0 * (this.cinematicTimer / this.cinematicTotal));

      if (this.cinematicTimer <= 0) {
        this.cinematicActive = false;
        this.shakeIntensity = 0;
        this.emit('cinematic_ended', { boss: this.boss });
      }
      // Note: During cinematic, boss can pause movement or emergence
      return;
    }

    if (!this.boss.alive) {
      if (!this.defeatedAnnounced) {
        this.defeatedAnnounced = true;
        this.emit('boss_defeated', {
          name: this.name,
          title: this.title,
          rewardGold: this.boss.reward,
          rewardXp: this.boss.xp
        });
      }
      return;
    }

    // Check enrage transition
    if (this.boss.enraged && !this.enrageAnnounced) {
      this.enrageAnnounced = true;
      this.shakeIntensity = 8.0; // short tremor on enrage
      this.emit('boss_enraged', {
        name: this.name,
        message: `${this.name} entrou em FÚRIA ANCESTRAL!`
      });
    }

    // Periodic shockwave ground stomp
    this.smashCooldown -= dt;
    if (this.smashCooldown <= 0) {
      this.smashCooldown = 7.0;
      this.performShockwave(battle);
    }
  }

  performShockwave(battle) {
    if (!this.boss.alive) return;
    this.shakeIntensity = 6.0;

    // Damage all towers slightly or push back/slow nearest hero effect
    this.emit('boss_shockwave', {
      x: this.boss.x,
      y: this.boss.y,
      radius: 180,
      damage: 15
    });
  }

  getHealthRatio() {
    if (!this.boss || this.boss.maxHp <= 0) return 0;
    return Math.max(0, this.boss.hp / this.boss.maxHp);
  }

  isCinematicActive() {
    return this.cinematicActive;
  }
}

if (typeof module !== 'undefined') {
  module.exports = { BossController };
}
