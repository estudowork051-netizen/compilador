const test = require('node:test');
const assert = require('node:assert');
const { ScreenRouter } = require('../src/core/router');
const { GameState } = require('../src/core/state');
const {
  MainMenuScreen,
  StageSelectScreen,
  HeroCollectionScreen,
  ShopScreen,
  SettingsScreen,
  BattleScreen
} = require('../src/ui/screens');

test('Navigation — Etapa 1: Router Initialization and Route Registration', () => {
  const router = new ScreenRouter({ defaultRoute: 'main_menu' });
  const gameState = new GameState();

  const screens = {
    main_menu: new MainMenuScreen(gameState),
    stage_select: new StageSelectScreen(gameState),
    hero_collection: new HeroCollectionScreen(gameState),
    shop: new ShopScreen(gameState),
    settings: new SettingsScreen(gameState),
    battle: new BattleScreen(gameState)
  };

  for (const [name, screen] of Object.entries(screens)) {
    router.registerRoute(name, screen);
    assert.strictEqual(router.hasRoute(name), true, `Route '${name}' should be registered`);
  }

  // Boot to default route
  router.navigate('main_menu');
  assert.strictEqual(router.getCurrentRoute(), 'main_menu');
});

test('Navigation — Etapa 1: Transitions, History Stack and Back Navigation', () => {
  const router = new ScreenRouter({ defaultRoute: 'main_menu' });
  const gameState = new GameState();

  router.registerRoute('main_menu', new MainMenuScreen(gameState));
  router.registerRoute('stage_select', new StageSelectScreen(gameState));
  router.registerRoute('hero_collection', new HeroCollectionScreen(gameState));
  router.registerRoute('shop', new ShopScreen(gameState));
  router.registerRoute('settings', new SettingsScreen(gameState));

  router.navigate('main_menu');
  assert.strictEqual(router.getCurrentRoute(), 'main_menu');
  assert.strictEqual(router.getHistory().length, 0);

  // Navigate to stage select
  router.navigate('stage_select');
  assert.strictEqual(router.getCurrentRoute(), 'stage_select');
  assert.strictEqual(router.getHistory().length, 1);
  assert.strictEqual(router.getHistory()[0].route, 'main_menu');

  // Navigate to shop from stage select
  router.navigate('shop');
  assert.strictEqual(router.getCurrentRoute(), 'shop');
  assert.strictEqual(router.getHistory().length, 2);

  // Back button returns to stage_select
  const back1 = router.back();
  assert.strictEqual(back1, true);
  assert.strictEqual(router.getCurrentRoute(), 'stage_select');
  assert.strictEqual(router.getHistory().length, 1);

  // Back button returns to main_menu
  const back2 = router.back();
  assert.strictEqual(back2, true);
  assert.strictEqual(router.getCurrentRoute(), 'main_menu');
  assert.strictEqual(router.getHistory().length, 0);

  // Extra back call on empty history falls back gracefully to default route without error
  const back3 = router.back();
  assert.strictEqual(back3, false);
  assert.strictEqual(router.getCurrentRoute(), 'main_menu');
});

test('Navigation — Etapa 1: Entering and Exiting Battle Lifecycle', () => {
  const router = new ScreenRouter({ defaultRoute: 'main_menu' });
  const gameState = new GameState();

  const battleScreen = new BattleScreen(gameState);
  router.registerRoute('main_menu', new MainMenuScreen(gameState));
  router.registerRoute('battle', battleScreen);

  router.navigate('main_menu');

  // Enter battle
  router.navigate('battle', { stageId: 1 });
  assert.strictEqual(router.getCurrentRoute(), 'battle');
  assert.ok(battleScreen.battle, 'Battle simulation should be initialized on enter');
  assert.strictEqual(battleScreen.battle.stageId, 1);

  // Exit battle back to menu
  router.navigate('main_menu');
  assert.strictEqual(router.getCurrentRoute(), 'main_menu');
  assert.strictEqual(battleScreen.battle, null, 'Battle simulation should be cleaned up on exit to avoid leaks');
});

test('Navigation — Etapa 1: Invalid Route Handling and Repeated Navigation', () => {
  const router = new ScreenRouter({ defaultRoute: 'main_menu' });
  const gameState = new GameState();

  router.registerRoute('main_menu', new MainMenuScreen(gameState));
  router.registerRoute('settings', new SettingsScreen(gameState));

  router.navigate('main_menu');

  // Attempt navigation to nonexistent route
  const res = router.navigate('non_existent_route');
  assert.strictEqual(res, false, 'Should safely reject unknown routes');
  assert.strictEqual(router.getCurrentRoute(), 'main_menu');

  // Repeatedly cycling between routes without crash
  for (let i = 0; i < 20; i++) {
    router.navigate('settings');
    assert.strictEqual(router.getCurrentRoute(), 'settings');
    router.navigate('main_menu');
    assert.strictEqual(router.getCurrentRoute(), 'main_menu');
  }
});
