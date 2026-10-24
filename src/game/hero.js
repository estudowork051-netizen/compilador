const { HEROES } = require('./config');

class BattleHero {
  constructor(heroId) {
    const config = HEROES[heroId];
    if (!config) {
      throw new Error(`[BattleHero] Unknown hero: ${heroId}`);
    }

    this.id = config.id;
    this.name = config.name;
    this.title = config.title;
    this.rarity = config.rarity;
    this.role = config.role;
    this.avatar = config.avatar;
    this.skill = { ...config.skill };
    this.stats = { ...config.stats };

    this.cooldownRemaining = 0;
    this.totalDamageDealt = 0;
    this.timesCast = 0;
  }

  update(dt) {
    if (this.cooldownRemaining > 0) {
      this.cooldownRemaining = Math.max(0, this.cooldownRemaining - dt);
    }
  }

  canCast() {
    return this.cooldownRemaining <= 0;
  }

  getCooldownRatio() {
    if (this.skill.cooldown <= 0) return 0;
    return this.cooldownRemaining / this.skill.cooldown;
  }

  cast(battle, targetCoords = null) {
    if (!this.canCast()) {
      return { success: false, reason: 'on_cooldown' };
    }

    this.cooldownRemaining = this.skill.cooldown;
    this.timesCast += 1;

    // Default target coords: center of enemy concentration or center of map
    const coords = targetCoords || this._findTargetCoords(battle);
    const result = {
      success: true,
      heroId: this.id,
      skillName: this.skill.name,
      x: coords.x,
      y: coords.y,
      affectedEnemies: 0,
      damageDealt: 0,
      details: ''
    };

    switch (this.id) {
      case 'kael': {
        // Explosão Arcana: 150 magic AoE damage in radius 180
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const d = Math.hypot(enemy.x - coords.x, enemy.y - coords.y);
          if (d <= this.skill.radius) {
            const actual = enemy.takeDamage(this.skill.damage, 0.4); // 40% magic penetration
            result.damageDealt += actual;
            result.affectedEnemies += 1;
          }
        }
        result.details = `${result.affectedEnemies} inimigos atingidos pela Explosão Arcana.`;
        break;
      }

      case 'lyra': {
        // Chuva de Flechas: 200 physical AoE damage in radius 200
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const d = Math.hypot(enemy.x - coords.x, enemy.y - coords.y);
          if (d <= this.skill.radius) {
            const actual = enemy.takeDamage(this.skill.damage, 0.1);
            result.damageDealt += actual;
            result.affectedEnemies += 1;
          }
        }
        result.details = `${result.affectedEnemies} atingidos pela Chuva de Flechas.`;
        break;
      }

      case 'brakk': {
        // Impacto Sísmico: 120 damage + 3s stun in radius 190
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const d = Math.hypot(enemy.x - coords.x, enemy.y - coords.y);
          if (d <= this.skill.radius) {
            const actual = enemy.takeDamage(this.skill.damage, 0.2);
            enemy.applyStun(this.skill.stunDuration);
            result.damageDealt += actual;
            result.affectedEnemies += 1;
          }
        }
        result.details = `${result.affectedEnemies} inimigos atordoados por 3s.`;
        break;
      }

      case 'nyra': {
        // Campo de Lentidão: 80 damage + 60% slow for 6s in radius 240
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const d = Math.hypot(enemy.x - coords.x, enemy.y - coords.y);
          if (d <= this.skill.radius) {
            const actual = enemy.takeDamage(this.skill.damage, 0.5);
            enemy.applySlow(this.skill.slowFactor, this.skill.duration);
            result.damageDealt += actual;
            result.affectedEnemies += 1;
          }
        }
        result.details = `${result.affectedEnemies} inimigos sob lentidão por 6s.`;
        break;
      }

      case 'tiko': {
        // Sobrecarga das Torres: +40% dmg, +60% speed for 7s to all towers
        for (const tower of battle.towers) {
          tower.applyBuff(this.skill.damageBuff, this.skill.speedBuff, this.skill.duration);
        }
        result.affectedEnemies = battle.towers.length;
        result.details = `${battle.towers.length} torres sobrecarregadas por 7s!`;
        break;
      }

      case 'vesper': {
        // Ruptura Dimensional: 450 damage to ALL enemies on battlefield
        for (const enemy of battle.enemies) {
          if (!enemy.alive) continue;
          const actual = enemy.takeDamage(this.skill.damage, 0.8);
          result.damageDealt += actual;
          result.affectedEnemies += 1;
        }
        result.details = `Ruptura Dimensional atingiu ${result.affectedEnemies} inimigos em todo o campo!`;
        break;
      }

      default:
        break;
    }

    this.totalDamageDealt += result.damageDealt;
    return result;
  }

  _findTargetCoords(battle) {
    // If there are alive enemies, find centroid of active enemies, else center of screen (270, 360)
    const alive = battle.enemies.filter(e => e.alive);
    if (alive.length === 0) {
      return { x: 270, y: 360 };
    }
    let sumX = 0;
    let sumY = 0;
    for (const e of alive) {
      sumX += e.x;
      sumY += e.y;
    }
    return {
      x: Math.round(sumX / alive.length),
      y: Math.round(sumY / alive.length)
    };
  }
}

if (typeof module !== 'undefined') {
  module.exports = { BattleHero };
}
