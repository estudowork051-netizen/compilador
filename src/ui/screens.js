const { STAGES, HEROES, TOWERS, RARITY } = require('../game/config');
const { BattleSimulation } = require('../game/battle');
const { CanvasRenderer } = require('../render/canvasRenderer');

/**
 * Screen Controllers implementation for full touch and event responsiveness.
 */

class MainMenuScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;

    const bind = (id, fn) => {
      const el = document.getElementById(id);
      if (el) {
        el.onclick = (e) => {
          e.preventDefault();
          fn();
        };
      }
    };

    bind('btn-menu-play', () => router.navigate('stage_select'));
    bind('btn-menu-continue', () => {
      const profile = this.gameState.getProfile();
      const highestUnlocked = Math.max(...profile.unlockedStages, 1);
      router.navigate('battle', { stageId: highestUnlocked });
    });
    bind('btn-menu-heroes', () => router.navigate('hero_collection'));
    bind('btn-menu-shop', () => router.navigate('shop'));
    bind('btn-menu-settings', () => router.navigate('settings'));
  }

  onEnter() {
    this.updateProfileDisplay();
  }

  onExit() {}

  updateProfileDisplay() {
    if (typeof document === 'undefined') return;
    const profile = this.gameState.getProfile();
    const lvlEl = document.getElementById('menu-player-level');
    const coinsEl = document.getElementById('menu-player-coins');
    const slotsEl = document.getElementById('menu-player-slots');

    if (lvlEl) lvlEl.textContent = `Nível ${profile.level}`;
    if (coinsEl) coinsEl.textContent = `${profile.coins} Ouro`;
    if (slotsEl) slotsEl.textContent = `${profile.selectedHeroes.length}/${this.gameState.getMaxHeroSlots()} Heróis`;
  }
}

class StageSelectScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;
    const backBtn = document.getElementById('btn-stage-back');
    if (backBtn) {
      backBtn.onclick = (e) => {
        e.preventDefault();
        router.back();
      };
    }
  }

  onEnter() {
    this.renderStages();
  }

  onExit() {}

  renderStages() {
    if (typeof document === 'undefined') return;
    const container = document.getElementById('stages-list');
    if (!container) return;

    container.innerHTML = '';
    const profile = this.gameState.getProfile();

    for (const stage of STAGES) {
      const isUnlocked = profile.unlockedStages.includes(stage.id);
      const isCompleted = profile.completedStages.includes(stage.id);

      const card = document.createElement('div');
      card.className = `stage-card ${isUnlocked ? 'unlocked' : 'locked'} ${isCompleted ? 'completed' : ''}`;

      card.innerHTML = `
        <div class="stage-info">
          <div class="stage-header">
            <span class="stage-num">Fase ${stage.id}</span>
            <span class="stage-theme-tag">${stage.theme.toUpperCase()}</span>
          </div>
          <div class="stage-title">${stage.name}</div>
          <div class="stage-desc">${stage.description}</div>
          <div class="stage-meta">
            <span>🛡️ Base: ${stage.baseHp}</span>
            <span>💰 Início: ${stage.startingGold}</span>
            <span>⭐ Recompensa: +${stage.rewardGold} Ouro / +${stage.rewardXp} XP</span>
          </div>
        </div>
        <div class="stage-action">
          ${isUnlocked
            ? `<button class="btn btn-primary btn-play-stage" data-id="${stage.id}">JOGAR</button>`
            : `<button class="btn btn-disabled" disabled>🔒 BLOQUEADA</button>`}
        </div>
      `;

      if (isUnlocked) {
        const btn = card.querySelector('.btn-play-stage');
        if (btn) {
          btn.onclick = (e) => {
            e.preventDefault();
            this.router.navigate('battle', { stageId: stage.id });
          };
        }
      }

      container.appendChild(card);
    }
  }
}

class HeroCollectionScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;
    const backBtn = document.getElementById('btn-heroes-back');
    if (backBtn) {
      backBtn.onclick = (e) => {
        e.preventDefault();
        router.back();
      };
    }
  }

  onEnter() {
    this.renderHeroes();
  }

  onExit() {}

  renderHeroes() {
    if (typeof document === 'undefined') return;
    const grid = document.getElementById('heroes-grid');
    const slotsEl = document.getElementById('heroes-slots-status');
    if (!grid) return;

    grid.innerHTML = '';
    const profile = this.gameState.getProfile();
    const maxSlots = this.gameState.getMaxHeroSlots();

    if (slotsEl) {
      slotsEl.textContent = `Equipe Selecionada: ${profile.selectedHeroes.length} / ${maxSlots} Espaços Desbloqueados`;
    }

    for (const [key, hero] of Object.entries(HEROES)) {
      const isUnlocked = profile.unlockedHeroes.includes(key);
      const isSelected = profile.selectedHeroes.includes(key);
      const rarityData = RARITY[hero.rarity];

      const card = document.createElement('div');
      card.className = `hero-card ${isUnlocked ? 'unlocked' : 'locked'} ${isSelected ? 'selected' : ''}`;
      card.style.borderColor = rarityData.border;

      card.innerHTML = `
        <div class="hero-rarity" style="color: ${rarityData.color}">
          <span>${rarityData.badge}</span> ${rarityData.name} — ${hero.role}
        </div>
        <div class="hero-main">
          <div class="hero-avatar" style="background: ${hero.iconColor}22; border-color: ${hero.iconColor}">
            ${hero.avatar}
          </div>
          <div class="hero-details">
            <div class="hero-name">${hero.name}</div>
            <div class="hero-title">${hero.title}</div>
            <div class="hero-skill-info">
              <strong>${hero.skill.name}:</strong> ${hero.skill.description}
            </div>
          </div>
        </div>
        <div class="hero-stats">
          <span>❤️ Vida: ${hero.stats.hp}</span>
          <span>⚔️ Ataque: ${hero.stats.attack}</span>
          <span>🛡️ Defesa: ${hero.stats.defense}</span>
          <span>⚡ Recarga: ${hero.skill.cooldown}s</span>
        </div>
        <div class="hero-card-footer">
          ${isUnlocked
            ? (isSelected
                ? `<button class="btn btn-secondary btn-deselect-hero" data-id="${key}">DESEQUIPAR</button>`
                : `<button class="btn btn-primary btn-select-hero" data-id="${key}">EQUIPAR</button>`)
            : `<div class="hero-locked-note">🔒 Desbloqueia no Nível ${hero.unlockLevel} ou Loja</div>`}
        </div>
      `;

      if (isUnlocked) {
        const selectBtn = card.querySelector('.btn-select-hero');
        const deselectBtn = card.querySelector('.btn-deselect-hero');

        if (selectBtn) {
          selectBtn.onclick = (e) => {
            e.preventDefault();
            const res = this.gameState.selectHero(key);
            if (!res.success && res.reason === 'slots_full') {
              alert(`Limite de heróis atingido! Você tem ${res.maxSlots} espaços disponíveis.`);
            }
            this.renderHeroes();
          };
        }
        if (deselectBtn) {
          deselectBtn.onclick = (e) => {
            e.preventDefault();
            this.gameState.deselectHero(key);
            this.renderHeroes();
          };
        }
      }

      grid.appendChild(card);
    }
  }
}

class ShopScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;
    const backBtn = document.getElementById('btn-shop-back');
    if (backBtn) {
      backBtn.onclick = (e) => {
        e.preventDefault();
        router.back();
      };
    }
  }

  onEnter() {
    this.renderShop();
  }

  onExit() {}

  renderShop() {
    if (typeof document === 'undefined') return;
    const container = document.getElementById('shop-items');
    const coinsEl = document.getElementById('shop-coins-counter');
    if (!container) return;

    const profile = this.gameState.getProfile();
    if (coinsEl) {
      coinsEl.textContent = `${profile.coins} Ouro`;
    }

    container.innerHTML = '';

    for (const [key, hero] of Object.entries(HEROES)) {
      if (hero.cost === 0) continue; // Initial free hero

      const isPurchased = profile.unlockedHeroes.includes(key);
      const canAfford = profile.coins >= hero.cost;
      const rarityData = RARITY[hero.rarity];

      const itemCard = document.createElement('div');
      itemCard.className = `shop-item-card ${isPurchased ? 'purchased' : ''}`;

      itemCard.innerHTML = `
        <div class="shop-hero-avatar" style="border-color: ${rarityData.border}">
          ${hero.avatar}
        </div>
        <div class="shop-hero-info">
          <div class="shop-hero-name">${hero.name} <span style="color: ${rarityData.color}">(${rarityData.name})</span></div>
          <div class="shop-hero-skill"><strong>${hero.skill.name}:</strong> ${hero.skill.description}</div>
          <div class="shop-hero-cost">💰 Custo: <strong>${hero.cost} Ouro</strong></div>
        </div>
        <div class="shop-item-action">
          ${isPurchased
            ? `<button class="btn btn-disabled" disabled>ADQUIRIDO</button>`
            : `<button class="btn ${canAfford ? 'btn-primary' : 'btn-disabled'} btn-buy-hero" data-id="${key}" ${canAfford ? '' : 'disabled'}>
                COMPRAR
              </button>`}
        </div>
      `;

      if (!isPurchased && canAfford) {
        const buyBtn = itemCard.querySelector('.btn-buy-hero');
        if (buyBtn) {
          buyBtn.onclick = (e) => {
            e.preventDefault();
            const spent = this.gameState.spendCoins(hero.cost);
            if (spent) {
              this.gameState.unlockHero(key);
              this.renderShop();
            }
          };
        }
      }

      container.appendChild(itemCard);
    }
  }
}

class SettingsScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;

    const backBtn = document.getElementById('btn-settings-back');
    if (backBtn) {
      backBtn.onclick = (e) => {
        e.preventDefault();
        router.back();
      };
    }

    const soundToggle = document.getElementById('setting-sound');
    if (soundToggle) {
      soundToggle.onchange = (e) => {
        this.gameState.updateSettings({ soundEnabled: e.target.checked });
      };
    }

    const musicToggle = document.getElementById('setting-music');
    if (musicToggle) {
      musicToggle.onchange = (e) => {
        this.gameState.updateSettings({ musicEnabled: e.target.checked });
      };
    }

    const resetBtn = document.getElementById('btn-reset-progress');
    if (resetBtn) {
      resetBtn.onclick = (e) => {
        e.preventDefault();
        if (confirm('Tem certeza que deseja apagar todo o progresso e reiniciar?')) {
          this.gameState.resetProgress();
          alert('Progresso reiniciado com sucesso!');
          this.router.navigate('main_menu');
        }
      };
    }
  }

  onEnter() {
    if (typeof document === 'undefined') return;
    const profile = this.gameState.getProfile();
    const soundToggle = document.getElementById('setting-sound');
    const musicToggle = document.getElementById('setting-music');

    if (soundToggle) soundToggle.checked = profile.settings.soundEnabled;
    if (musicToggle) musicToggle.checked = profile.settings.musicEnabled;
  }

  onExit() {}
}

class BattleScreen {
  constructor(gameState) {
    this.gameState = gameState;
    this.router = null;
    this.battle = null;
    this.renderer = null;
    this.rafId = null;
    this.lastFrameTime = 0;
    this.selectedSpotId = null;
  }

  init(router) {
    this.router = router;
    if (typeof document === 'undefined') return;

    // HUD bindings
    const bind = (id, fn) => {
      const el = document.getElementById(id);
      if (el) {
        el.onclick = (e) => {
          e.preventDefault();
          fn();
        };
      }
    };

    bind('btn-battle-pause', () => this.togglePause());
    bind('btn-battle-speed', () => this.toggleSpeed());
    bind('btn-pause-resume', () => this.resume());
    bind('btn-pause-restart', () => this.restart());
    bind('btn-pause-quit', () => this.quit());
    bind('btn-victory-advance', () => this.onVictoryAdvance());
    bind('btn-defeat-retry', () => this.restart());
    bind('btn-defeat-quit', () => this.quit());

    // Tower build actions
    bind('btn-build-archer', () => this.buildTower('archer'));
    bind('btn-build-magic', () => this.buildTower('magic'));
    bind('btn-build-cannon', () => this.buildTower('cannon'));
    bind('btn-upgrade-tower', () => this.upgradeSelectedTower());
    bind('btn-sell-tower', () => this.sellSelectedTower());
    bind('btn-close-tower-modal', () => this.closeTowerModal());

    // Canvas click handling
    const canvas = document.getElementById('battle-canvas');
    if (canvas) {
      canvas.addEventListener('click', (e) => this.onCanvasClick(e));
      canvas.addEventListener('touchstart', (e) => {
        if (e.touches && e.touches.length > 0) {
          this.onCanvasTouch(e.touches[0]);
        }
      }, { passive: true });
    }
  }

  onEnter(params = {}) {
    const stageId = params.stageId || 1;
    const profile = this.gameState.getProfile();
    const selectedHeroes = profile.selectedHeroes.length > 0 ? profile.selectedHeroes : ['kael'];

    this.battle = new BattleSimulation(stageId, selectedHeroes, {
      timeScale: profile.settings.gameSpeed || 1
    });

    // Setup canvas renderer
    if (typeof document !== 'undefined') {
      const canvas = document.getElementById('battle-canvas');
      if (canvas) {
        this.renderer = new CanvasRenderer(canvas);
      }
      this._hideAllModals();
      this.renderHeroActionBar();
    }

    // Bind simulation events
    this._bindBattleEvents();

    this.battle.start();
    this.lastFrameTime = performance.now();
    this._startLoop();
  }

  onExit() {
    this._stopLoop();
    if (this.battle) {
      this.battle.pause();
      this.battle = null;
    }
    this.selectedSpotId = null;
  }

  _bindBattleEvents() {
    this.battle.on('victory', (data) => this.handleVictory(data));
    this.battle.on('defeat', (data) => this.handleDefeat(data));
    this.battle.on('base_damaged', () => this.updateHUD());
    this.battle.on('gold_changed', () => this.updateHUD());
    this.battle.on('wave_started', () => this.updateHUD());
  }

  _startLoop() {
    if (typeof requestAnimationFrame === 'undefined') return;

    const loop = (timestamp) => {
      if (!this.battle) return;

      const dt = (timestamp - this.lastFrameTime) / 1000;
      this.lastFrameTime = timestamp;

      this.battle.tick(dt);

      if (this.renderer) {
        this.renderer.render(this.battle, { selectedSpotId: this.selectedSpotId });
      }

      this.updateHUD();
      this.updateHeroCooldowns();

      if (!this.battle.isOver()) {
        this.rafId = requestAnimationFrame(loop);
      }
    };

    this.rafId = requestAnimationFrame(loop);
  }

  _stopLoop() {
    if (this.rafId && typeof cancelAnimationFrame !== 'undefined') {
      cancelAnimationFrame(this.rafId);
      this.rafId = null;
    }
  }

  togglePause() {
    if (!this.battle) return;
    if (this.battle.state === 'running') {
      this.battle.pause();
      this._showModal('modal-pause');
    } else if (this.battle.state === 'paused') {
      this.resume();
    }
  }

  resume() {
    if (this.battle && this.battle.state === 'paused') {
      this.battle.resume();
      this._hideModal('modal-pause');
    }
  }

  restart() {
    this._hideAllModals();
    const stageId = this.battle ? this.battle.stageId : 1;
    this.onExit();
    this.onEnter({ stageId });
  }

  quit() {
    this._hideAllModals();
    this.onExit();
    this.router.navigate('main_menu');
  }

  toggleSpeed() {
    if (!this.battle) return;
    const newSpeed = this.battle.timeScale === 1 ? 2 : 1;
    this.battle.setSpeed(newSpeed);
    const speedBtn = document.getElementById('btn-battle-speed');
    if (speedBtn) speedBtn.textContent = `${newSpeed}x`;
  }

  onCanvasClick(e) {
    if (typeof document === 'undefined') return;
    const canvas = document.getElementById('battle-canvas');
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const clickX = (e.clientX - rect.left) * scaleX;
    const clickY = (e.clientY - rect.top) * scaleY;

    this._handleFieldInteraction(clickX, clickY);
  }

  onCanvasTouch(touch) {
    if (typeof document === 'undefined') return;
    const canvas = document.getElementById('battle-canvas');
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const clickX = (touch.clientX - rect.left) * scaleX;
    const clickY = (touch.clientY - rect.top) * scaleY;

    this._handleFieldInteraction(clickX, clickY);
  }

  _handleFieldInteraction(x, y) {
    if (!this.battle) return;

    // Check click on build spots
    for (const spot of this.battle.buildSpots) {
      const dist = Math.hypot(spot.x - x, spot.y - y);
      if (dist <= 32) {
        this.selectedSpotId = spot.id;
        this._showTowerPanel(spot);
        return;
      }
    }

    // Clicked elsewhere on field -> dismiss build panel
    this.selectedSpotId = null;
    this.closeTowerModal();
  }

  _showTowerPanel(spot) {
    if (typeof document === 'undefined') return;
    const modal = document.getElementById('modal-tower-actions');
    if (!modal) return;

    const buildActions = document.getElementById('tower-build-options');
    const manageActions = document.getElementById('tower-manage-options');

    if (spot.tower) {
      // Existing tower: show upgrade and sell
      if (buildActions) buildActions.style.display = 'none';
      if (manageActions) manageActions.style.display = 'flex';

      const tower = spot.tower;
      const upgradeBtn = document.getElementById('btn-upgrade-tower');
      const sellBtn = document.getElementById('btn-sell-tower');
      const infoEl = document.getElementById('tower-manage-info');

      if (infoEl) {
        infoEl.textContent = `${tower.name} (Nível ${tower.level}) — Dano: ${tower.effectiveDamage} | Alcance: ${tower.effectiveRange}`;
      }

      if (upgradeBtn) {
        if (tower.canUpgrade()) {
          const cost = tower.getUpgradeCost();
          upgradeBtn.textContent = `Melhorar (+Nv.${tower.level + 1}) — 💰 ${cost}`;
          upgradeBtn.disabled = this.battle.gold < cost;
        } else {
          upgradeBtn.textContent = 'Nível Máximo Atingido';
          upgradeBtn.disabled = true;
        }
      }

      if (sellBtn) {
        sellBtn.textContent = `Vender (+💰 ${tower.getSellValue()})`;
      }
    } else {
      // Empty spot: show tower choices
      if (buildActions) buildActions.style.display = 'flex';
      if (manageActions) manageActions.style.display = 'none';

      // Update affordability on buttons
      const updateCostBtn = (id, type) => {
        const btn = document.getElementById(id);
        if (btn) {
          const cost = TOWERS[type].levels[0].cost;
          btn.disabled = this.battle.gold < cost;
        }
      };
      updateCostBtn('btn-build-archer', 'archer');
      updateCostBtn('btn-build-magic', 'magic');
      updateCostBtn('btn-build-cannon', 'cannon');
    }

    modal.style.display = 'flex';
  }

  closeTowerModal() {
    this.selectedSpotId = null;
    this._hideModal('modal-tower-actions');
  }

  buildTower(type) {
    if (!this.battle || !this.selectedSpotId) return;
    const res = this.battle.buildTower(this.selectedSpotId, type);
    if (res.success) {
      this.closeTowerModal();
    }
  }

  upgradeSelectedTower() {
    if (!this.battle || !this.selectedSpotId) return;
    const res = this.battle.upgradeTower(this.selectedSpotId);
    if (res.success) {
      const spot = this.battle.buildSpots.find(s => s.id === this.selectedSpotId);
      if (spot) this._showTowerPanel(spot);
    }
  }

  sellSelectedTower() {
    if (!this.battle || !this.selectedSpotId) return;
    const res = this.battle.sellTower(this.selectedSpotId);
    if (res.success) {
      this.closeTowerModal();
    }
  }

  renderHeroActionBar() {
    if (typeof document === 'undefined') return;
    const bar = document.getElementById('battle-hero-bar');
    if (!bar || !this.battle) return;

    bar.innerHTML = '';
    for (const hero of this.battle.heroes) {
      const btn = document.createElement('button');
      btn.className = 'hero-skill-btn';
      btn.id = `hero-btn-${hero.id}`;
      btn.innerHTML = `
        <div class="hero-skill-avatar">${hero.avatar}</div>
        <div class="hero-skill-name">${hero.name}</div>
        <div class="hero-cooldown-overlay" id="cd-${hero.id}"></div>
      `;

      btn.onclick = (e) => {
        e.preventDefault();
        this.battle.castHeroSkill(hero.id);
      };

      bar.appendChild(btn);
    }
  }

  updateHeroCooldowns() {
    if (typeof document === 'undefined' || !this.battle) return;
    for (const hero of this.battle.heroes) {
      const cdEl = document.getElementById(`cd-${hero.id}`);
      const btn = document.getElementById(`hero-btn-${hero.id}`);
      if (!cdEl || !btn) continue;

      const ratio = hero.getCooldownRatio();
      if (ratio > 0) {
        cdEl.style.height = `${Math.round(ratio * 100)}%`;
        btn.classList.add('on-cooldown');
      } else {
        cdEl.style.height = '0%';
        btn.classList.remove('on-cooldown');
      }
    }
  }

  updateHUD() {
    if (typeof document === 'undefined' || !this.battle) return;
    const baseHpEl = document.getElementById('hud-base-hp');
    const goldEl = document.getElementById('hud-gold');
    const waveEl = document.getElementById('hud-wave');
    const bossHud = document.getElementById('hud-boss-container');
    const bossBar = document.getElementById('hud-boss-fill');

    if (baseHpEl) {
      baseHpEl.textContent = `${this.battle.baseHp}/${this.battle.maxBaseHp}`;
    }
    if (goldEl) {
      goldEl.textContent = `${this.battle.gold}`;
    }
    if (waveEl) {
      waveEl.textContent = `Onda ${this.battle.currentWaveIndex}/${this.battle.totalWaves}`;
    }

    // Boss bar visibility
    if (bossHud && bossBar) {
      if (this.battle.bossController && this.battle.bossController.boss.alive) {
        bossHud.style.display = 'block';
        const ratio = this.battle.bossController.getHealthRatio();
        bossBar.style.width = `${Math.round(ratio * 100)}%`;
      } else {
        bossHud.style.display = 'none';
      }
    }
  }

  handleVictory(data) {
    this._stopLoop();
    this.gameState.addCoins(data.rewardGold);
    const xpRes = this.gameState.addXp(data.rewardXp);
    this.gameState.completeStage(data.stageId);

    if (typeof document !== 'undefined') {
      const goldRewardEl = document.getElementById('victory-gold-reward');
      const xpRewardEl = document.getElementById('victory-xp-reward');
      const levelUpNote = document.getElementById('victory-levelup-note');

      if (goldRewardEl) goldRewardEl.textContent = `+${data.rewardGold} Ouro`;
      if (xpRewardEl) xpRewardEl.textContent = `+${data.rewardXp} XP`;
      if (levelUpNote) {
        if (xpRes.levelUps > 0) {
          levelUpNote.textContent = `🎉 SUBIU PARA O NÍVEL ${xpRes.newLevel}! Novos heróis ou espaços desbloqueados!`;
          levelUpNote.style.display = 'block';
        } else {
          levelUpNote.style.display = 'none';
        }
      }

      this._showModal('modal-victory');
    }
  }

  handleDefeat(data) {
    this._stopLoop();
    if (typeof document !== 'undefined') {
      const summaryEl = document.getElementById('defeat-summary');
      if (summaryEl) {
        summaryEl.textContent = `Você resistiu até a Onda ${data.waveReached}/${data.totalWaves} e derrotou ${data.enemiesDefeated} inimigos.`;
      }
      this._showModal('modal-defeat');
    }
  }

  onVictoryAdvance() {
    this._hideAllModals();
    this.onExit();
    this.router.navigate('stage_select');
  }

  _showModal(id) {
    if (typeof document === 'undefined') return;
    const modal = document.getElementById(id);
    if (modal) modal.style.display = 'flex';
  }

  _hideModal(id) {
    if (typeof document === 'undefined') return;
    const modal = document.getElementById(id);
    if (modal) modal.style.display = 'none';
  }

  _hideAllModals() {
    if (typeof document === 'undefined') return;
    const modals = document.querySelectorAll('.game-modal');
    modals.forEach(m => m.style.display = 'none');
  }
}

if (typeof module !== 'undefined') {
  module.exports = {
    MainMenuScreen,
    StageSelectScreen,
    HeroCollectionScreen,
    ShopScreen,
    SettingsScreen,
    BattleScreen
  };
}
