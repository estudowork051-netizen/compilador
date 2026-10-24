const fs = require('fs');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const srcDir = path.join(projectRoot, 'src');
const publicDir = path.join(projectRoot, 'public');
const androidAssetsDir = path.join(projectRoot, 'android/app/src/main/assets/www');

const fileList = [
  'core/events.js',
  'core/storage.js',
  'game/config.js',
  'core/state.js',
  'core/router.js',
  'game/path.js',
  'game/enemy.js',
  'game/projectile.js',
  'game/tower.js',
  'game/hero.js',
  'game/boss.js',
  'game/particles.js',
  'game/battle.js',
  'render/canvasRenderer.js',
  'ui/screens.js',
  'index.js'
];

function bundle() {
  console.log('[Bundler] Packing CommonJS modules into standalone client bundle...');
  const moduleEntries = [];

  for (const relPath of fileList) {
    const fullPath = path.join(srcDir, relPath);
    const content = fs.readFileSync(fullPath, 'utf8');
    const moduleId = relPath.replace(/\.js$/, '');

    moduleEntries.push(`
  "${moduleId}": function(module, exports, require) {
${content}
  }`);
  }

  const bundleSource = `/**
 * BUNDLED BROWSER / ANDROID WEBVIEW RUNTIME
 * Generated automatically by RPG Studio Bundler
 */
(function() {
  'use strict';
  const modules = {${moduleEntries.join(',\n')}
  };
  const cache = {};

  function normalizePath(base, rel) {
    if (!rel.startsWith('.')) return rel;
    const baseParts = base.split('/').slice(0, -1);
    const relParts = rel.split('/');
    for (const p of relParts) {
      if (p === '.') continue;
      if (p === '..') {
        baseParts.pop();
      } else {
        baseParts.push(p);
      }
    }
    return baseParts.join('/').replace(/\\.js$/, '');
  }

  function makeRequire(currentModuleId) {
    return function(id) {
      const resolved = normalizePath(currentModuleId, id);
      if (cache[resolved]) {
        return cache[resolved].exports;
      }
      if (!modules[resolved]) {
        throw new Error("[Bundle Runtime] Cannot find module '" + id + "' (resolved as '" + resolved + "') from '" + currentModuleId + "'");
      }
      const mod = { exports: {} };
      cache[resolved] = mod;
      modules[resolved](mod, mod.exports, makeRequire(resolved));
      return mod.exports;
    };
  }

  // Bootstrap entry point
  const rootRequire = makeRequire('root');
  rootRequire('index');
})();
`;

  // Write to public/
  fs.mkdirSync(publicDir, { recursive: true });
  fs.writeFileSync(path.join(publicDir, 'bundle.js'), bundleSource, 'utf8');
  console.log('[Bundler] Successfully generated public/bundle.js');

  // Copy to android/app/src/main/assets/www/
  fs.mkdirSync(androidAssetsDir, { recursive: true });
  fs.writeFileSync(path.join(androidAssetsDir, 'bundle.js'), bundleSource, 'utf8');

  // Copy HTML and CSS
  fs.copyFileSync(path.join(publicDir, 'index.html'), path.join(androidAssetsDir, 'index.html'));
  fs.copyFileSync(path.join(publicDir, 'style.css'), path.join(androidAssetsDir, 'style.css'));
  console.log('[Bundler] Synced assets to Android assets folder:', androidAssetsDir);
}

bundle();
