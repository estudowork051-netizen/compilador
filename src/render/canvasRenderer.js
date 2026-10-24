/**
 * High Performance Canvas 2D Renderer for Medieval Fantasy Tower Defense.
 */

class CanvasRenderer {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d');
    this.width = canvas.width || 540;
    this.height = canvas.height || 720;
    this.selectedSpot = null;
  }

  resize(w, h) {
    this.width = w;
    this.height = h;
    this.canvas.width = w;
    this.canvas.height = h;
  }

  render(battle, options = {}) {
    if (!this.ctx) return;
    const ctx = this.ctx;

    ctx.save();

    // 1. Screen Shake offset
    if (battle.screenShake > 0) {
      const sx = (Math.random() - 0.5) * battle.screenShake * 2;
      const sy = (Math.random() - 0.5) * battle.screenShake * 2;
      ctx.translate(sx, sy);
    }

    // 2. Clear & Draw Map Scenery
    this._drawBackground(battle.stageConfig.theme);
    this._drawPath(battle.waypoints, battle.stageConfig.theme);
    this._drawBase(battle.waypoints[battle.waypoints.length - 1]);

    // 3. Draw Build Spots
    this._drawBuildSpots(battle.buildSpots, options.selectedSpotId);

    // 4. Draw Towers
    for (const tower of battle.towers) {
      this._drawTower(tower, tower.spotId === options.selectedSpotId);
    }

    // 5. Draw Enemies
    for (const enemy of battle.enemies) {
      this._drawEnemy(enemy);
    }

    // 6. Draw Projectiles
    for (const proj of battle.projectiles) {
      this._drawProjectile(proj);
    }

    // 7. Draw Particles & Floating Text
    this._drawParticles(battle.particles);

    // 8. Draw Range Indicator if spot selected
    if (options.selectedSpotId) {
      const spot = battle.buildSpots.find(s => s.id === options.selectedSpotId);
      if (spot) {
        this._drawRangeIndicator(spot);
      }
    }

    // 9. Draw Boss Banner / Cinematic Overlay
    if (battle.bossController && battle.bossController.isCinematicActive()) {
      this._drawBossCinematic(battle.bossController);
    }

    ctx.restore();
  }

  _drawBackground(theme) {
    const ctx = this.ctx;
    let bgGrad = ctx.createLinearGradient(0, 0, 0, this.height);

    if (theme === 'ruins') {
      bgGrad.addColorStop(0, '#263238');
      bgGrad.addColorStop(1, '#1a1f23');
    } else if (theme === 'volcano') {
      bgGrad.addColorStop(0, '#3e2723');
      bgGrad.addColorStop(1, '#1b0000');
    } else {
      // Forest
      bgGrad.addColorStop(0, '#1b3820');
      bgGrad.addColorStop(1, '#0e2013');
    }

    ctx.fillStyle = bgGrad;
    ctx.fillRect(0, 0, this.width, this.height);

    // Decorative grid/grass blades
    ctx.fillStyle = 'rgba(255, 255, 255, 0.02)';
    for (let x = 0; x < this.width; x += 45) {
      ctx.fillRect(x, 0, 1, this.height);
    }
    for (let y = 0; y < this.height; y += 45) {
      ctx.fillRect(0, y, this.width, 1);
    }
  }

  _drawPath(waypoints, theme) {
    if (!waypoints || waypoints.length < 2) return;
    const ctx = this.ctx;

    // Path border / trench
    ctx.beginPath();
    ctx.moveTo(waypoints[0].x, waypoints[0].y);
    for (let i = 1; i < waypoints.length; i++) {
      ctx.lineTo(waypoints[i].x, waypoints[i].y);
    }
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.lineWidth = 44;
    ctx.strokeStyle = theme === 'ruins' ? '#37474f' : (theme === 'volcano' ? '#bf360c' : '#332314');
    ctx.stroke();

    // Inner path gravel / stone
    ctx.lineWidth = 36;
    ctx.strokeStyle = theme === 'ruins' ? '#546e7a' : (theme === 'volcano' ? '#4e342e' : '#5d4037');
    ctx.stroke();

    // Center dotted trail
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 12]);
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.stroke();
    ctx.setLineDash([]);
  }

  _drawBase(basePoint) {
    if (!basePoint) return;
    const ctx = this.ctx;

    // Glowing defensive portal / castle keep
    ctx.save();
    ctx.translate(basePoint.x, basePoint.y);

    const grad = ctx.createRadialGradient(0, 0, 5, 0, 0, 36);
    grad.addColorStop(0, '#42a5f5');
    grad.addColorStop(0.5, '#1565c0');
    grad.addColorStop(1, 'transparent');

    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.arc(0, 0, 36, 0, Math.PI * 2);
    ctx.fill();

    // Base icon
    ctx.font = '24px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('🏰', 0, 0);

    ctx.restore();
  }

  _drawBuildSpots(spots, selectedSpotId) {
    const ctx = this.ctx;
    for (const spot of spots) {
      if (spot.tower) continue; // Tower rendered separately

      ctx.save();
      ctx.translate(spot.x, spot.y);

      const isSelected = spot.id === selectedSpotId;

      // Pedestal base
      ctx.fillStyle = isSelected ? '#ffb300' : 'rgba(255, 255, 255, 0.12)';
      ctx.beginPath();
      ctx.arc(0, 0, 24, 0, Math.PI * 2);
      ctx.fill();

      // Border ring
      ctx.strokeStyle = isSelected ? '#ff6f00' : 'rgba(255, 255, 255, 0.35)';
      ctx.lineWidth = isSelected ? 3 : 2;
      ctx.setLineDash([4, 4]);
      ctx.stroke();
      ctx.setLineDash([]);

      // Hammer/plus icon
      ctx.fillStyle = isSelected ? '#212121' : 'rgba(255, 255, 255, 0.6)';
      ctx.font = '16px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('➕', 0, 0);

      ctx.restore();
    }
  }

  _drawTower(tower, isSelected) {
    const ctx = this.ctx;
    ctx.save();
    ctx.translate(tower.x, tower.y);

    // Selection ring
    if (isSelected) {
      ctx.strokeStyle = '#ffd54f';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(0, 0, 28, 0, Math.PI * 2);
      ctx.stroke();
    }

    // Overclock aura (Tiko buff)
    if (tower.buffTimer > 0) {
      ctx.strokeStyle = '#ff7043';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(0, 0, 32 + Math.sin(Date.now() / 150) * 3, 0, Math.PI * 2);
      ctx.stroke();
    }

    // Pedestal
    ctx.fillStyle = '#263238';
    ctx.beginPath();
    ctx.arc(0, 0, 24, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = tower.config.color;
    ctx.lineWidth = 2;
    ctx.stroke();

    // Tower Icon
    ctx.font = '22px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(tower.config.avatar, 0, -2);

    // Level Badge
    ctx.fillStyle = '#212121';
    ctx.beginPath();
    ctx.arc(14, 14, 9, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#ffd54f';
    ctx.font = 'bold 11px sans-serif';
    ctx.fillText(tower.level.toString(), 14, 14);

    ctx.restore();
  }

  _drawEnemy(enemy) {
    const ctx = this.ctx;
    ctx.save();
    ctx.translate(enemy.x, enemy.y);

    // Stun / Slow aura
    if (enemy.stunTimer > 0) {
      ctx.strokeStyle = '#ffee58';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(0, 0, enemy.size + 6, 0, Math.PI * 2);
      ctx.stroke();
    } else if (enemy.slowTimer > 0) {
      ctx.fillStyle = 'rgba(126, 87, 194, 0.35)';
      ctx.beginPath();
      ctx.arc(0, 0, enemy.size + 4, 0, Math.PI * 2);
      ctx.fill();
    }

    // Boss Enrage aura
    if (enemy.isBoss && enemy.enraged) {
      ctx.fillStyle = 'rgba(213, 0, 0, 0.3)';
      ctx.beginPath();
      ctx.arc(0, 0, enemy.size + 10, 0, Math.PI * 2);
      ctx.fill();
    }

    // Body
    ctx.fillStyle = enemy.color;
    ctx.beginPath();
    ctx.arc(0, 0, enemy.size, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#212121';
    ctx.lineWidth = enemy.isBoss ? 3 : 1.5;
    ctx.stroke();

    // Enemy Icon/Avatar
    ctx.font = `${Math.round(enemy.size * 1.2)}px sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const icon = enemy.isBoss ? '👹' : (enemy.type === 'golem' ? '🗿' : (enemy.type === 'orc' ? '🧌' : '👺'));
    ctx.fillText(icon, 0, 0);

    // Health Bar (for non-boss, boss has top health bar)
    if (!enemy.isBoss) {
      const barW = Math.max(28, enemy.size * 2);
      const barH = 4;
      const barY = -enemy.size - 8;
      const hpRatio = Math.max(0, enemy.hp / enemy.maxHp);

      ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
      ctx.fillRect(-barW / 2, barY, barW, barH);
      ctx.fillStyle = hpRatio > 0.5 ? '#66bb6a' : (hpRatio > 0.25 ? '#ffa726' : '#ef5350');
      ctx.fillRect(-barW / 2, barY, barW * hpRatio, barH);
    }

    ctx.restore();
  }

  _drawProjectile(proj) {
    const ctx = this.ctx;
    ctx.save();
    ctx.translate(proj.x, proj.y);

    ctx.fillStyle = proj.color;
    ctx.shadowColor = proj.color;
    ctx.shadowBlur = 6;
    ctx.beginPath();
    ctx.arc(0, 0, proj.radius, 0, Math.PI * 2);
    ctx.fill();

    ctx.restore();
  }

  _drawParticles(particles) {
    const ctx = this.ctx;

    // Particles
    for (const p of particles.particles) {
      ctx.save();
      ctx.globalAlpha = p.alpha;
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }

    // Floating combat text
    ctx.save();
    ctx.font = 'bold 13px sans-serif';
    ctx.textAlign = 'center';
    for (const f of particles.floats) {
      ctx.globalAlpha = f.alpha;
      ctx.fillStyle = f.color;
      ctx.shadowColor = '#000';
      ctx.shadowBlur = 3;
      ctx.fillText(f.text, f.x, f.y);
    }
    ctx.restore();
  }

  _drawRangeIndicator(spot) {
    const ctx = this.ctx;
    const tower = spot.tower;
    const range = tower ? tower.effectiveRange : 135;

    ctx.save();
    ctx.translate(spot.x, spot.y);
    ctx.fillStyle = 'rgba(66, 165, 245, 0.12)';
    ctx.strokeStyle = 'rgba(66, 165, 245, 0.5)';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.arc(0, 0, range, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.restore();
  }

  _drawBossCinematic(bossController) {
    const ctx = this.ctx;
    ctx.save();

    // Darken background
    ctx.fillStyle = 'rgba(0, 0, 0, 0.65)';
    ctx.fillRect(0, 0, this.width, this.height);

    // Ornate Boss Title Banner
    const cy = this.height * 0.35;
    ctx.fillStyle = 'rgba(183, 28, 28, 0.9)';
    ctx.fillRect(0, cy - 45, this.width, 90);

    ctx.strokeStyle = '#ffd700';
    ctx.lineWidth = 3;
    ctx.strokeRect(0, cy - 45, this.width, 90);

    ctx.textAlign = 'center';
    ctx.fillStyle = '#ffcdd2';
    ctx.font = '14px sans-serif';
    ctx.fillText('⚠️ CHEFE ANCESTRAL DESPERTOU ⚠️', this.width / 2, cy - 20);

    ctx.fillStyle = '#fff';
    ctx.font = 'bold 22px sans-serif';
    ctx.shadowColor = '#000';
    ctx.shadowBlur = 6;
    ctx.fillText(bossController.name, this.width / 2, cy + 6);

    ctx.fillStyle = '#ffd54f';
    ctx.font = 'italic 14px sans-serif';
    ctx.fillText(`— ${bossController.title} —`, this.width / 2, cy + 28);

    ctx.restore();
  }
}

if (typeof module !== 'undefined') {
  module.exports = { CanvasRenderer };
}
