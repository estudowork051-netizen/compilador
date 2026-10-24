const { GameState } = require('./core/state');
const { ScreenRouter } = require('./core/router');
const {
  MainMenuScreen,
  StageSelectScreen,
  HeroCollectionScreen,
  ShopScreen,
  SettingsScreen,
  BattleScreen
} = require('./ui/screens');

function initializeGame() {
  const gameState = new GameState();
  const router = new ScreenRouter({ defaultRoute: 'main_menu' });

  // Register screens
  router.registerRoute('main_menu', new MainMenuScreen(gameState));
  router.registerRoute('stage_select', new StageSelectScreen(gameState));
  router.registerRoute('hero_collection', new HeroCollectionScreen(gameState));
  router.registerRoute('shop', new ShopScreen(gameState));
  router.registerRoute('settings', new SettingsScreen(gameState));
  router.registerRoute('battle', new BattleScreen(gameState));

  // Boot up into main menu
  router.navigate('main_menu');

  const instance = { gameState, router };

  if (typeof window !== 'undefined') {
    window.game = instance;
  }

  return instance;
}

if (typeof window !== 'undefined') {
  window.addEventListener('DOMContentLoaded', () => {
    initializeGame();
  });
}

if (typeof module !== 'undefined') {
  module.exports = { initializeGame };
}
