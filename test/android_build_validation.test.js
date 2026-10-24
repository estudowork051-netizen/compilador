const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const androidDir = path.join(rootDir, 'android');
const appDir = path.join(androidDir, 'app');
const resDir = path.join(appDir, 'src/main/res');

test('Android Configuration — Root Build & Settings Validation', () => {
  const settingsPath = path.join(androidDir, 'settings.gradle');
  const rootBuildPath = path.join(androidDir, 'build.gradle');
  const gradlewPath = path.join(androidDir, 'gradlew');

  assert.ok(fs.existsSync(settingsPath), 'settings.gradle must exist');
  assert.ok(fs.existsSync(rootBuildPath), 'root build.gradle must exist');
  assert.ok(fs.existsSync(gradlewPath), 'gradlew script must exist');

  // Verify gradlew permissions
  const stats = fs.statSync(gradlewPath);
  assert.ok((stats.mode & 0o111) !== 0, 'gradlew must have executable permissions');

  // Verify root build.gradle does not use deprecated buildDir
  const rootBuild = fs.readFileSync(rootBuildPath, 'utf8');
  assert.ok(!rootBuild.includes('rootProject.buildDir'), 'root build.gradle must not use deprecated rootProject.buildDir');
  assert.ok(rootBuild.includes('rootProject.layout.buildDirectory'), 'root build.gradle must use rootProject.layout.buildDirectory');
});

test('Android Configuration — App Gradle & ProGuard Validation', () => {
  const appBuildPath = path.join(appDir, 'build.gradle');
  const proguardPath = path.join(appDir, 'proguard-rules.pro');

  assert.ok(fs.existsSync(appBuildPath), 'app/build.gradle must exist');
  assert.ok(fs.existsSync(proguardPath), 'app/proguard-rules.pro must exist');

  const appBuild = fs.readFileSync(appBuildPath, 'utf8');
  assert.ok(appBuild.includes("compileSdk 34"), 'compileSdk must be 34');
  assert.ok(appBuild.includes("minSdk 24"), 'minSdk must be 24');
  assert.ok(appBuild.includes("targetSdk 34"), 'targetSdk must be 34');
  assert.ok(appBuild.includes("namespace 'com.rpg.towerdefense'"), 'namespace must be com.rpg.towerdefense');
  assert.ok(appBuild.includes("signingConfigs"), 'signingConfigs must be declared');
  assert.ok(appBuild.includes("proguard-rules.pro"), 'proguardFiles must reference proguard-rules.pro');
});

test('Android Configuration — Manifest & Resource Integrity', () => {
  const manifestPath = path.join(appDir, 'src/main/AndroidManifest.xml');
  assert.ok(fs.existsSync(manifestPath), 'AndroidManifest.xml must exist');

  const manifest = fs.readFileSync(manifestPath, 'utf8');
  assert.ok(manifest.includes('android:screenOrientation="portrait"'), 'Orientation must be locked to portrait');
  assert.ok(manifest.includes('android:hardwareAccelerated="true"'), 'Hardware acceleration must be enabled');
  assert.ok(manifest.includes('.MainActivity'), 'MainActivity must be declared');

  // Check referenced resources exist
  assert.ok(fs.existsSync(path.join(resDir, 'values/strings.xml')), 'strings.xml must exist');
  assert.ok(fs.existsSync(path.join(resDir, 'values/styles.xml')), 'styles.xml must exist');

  // Check strings content
  const strings = fs.readFileSync(path.join(resDir, 'values/strings.xml'), 'utf8');
  assert.ok(strings.includes('name="app_name"'), 'strings.xml must define app_name');

  // Check styles content
  const styles = fs.readFileSync(path.join(resDir, 'values/styles.xml'), 'utf8');
  assert.ok(styles.includes('Theme.TowerDefenseRPG'), 'styles.xml must define Theme.TowerDefenseRPG');
  assert.ok(styles.includes('Theme.TowerDefenseRPG.Fullscreen'), 'styles.xml must define Theme.TowerDefenseRPG.Fullscreen');

  // Check icons existence for AAPT2
  assert.ok(fs.existsSync(path.join(resDir, 'drawable/ic_launcher_background.xml')), 'ic_launcher_background.xml must exist');
  assert.ok(fs.existsSync(path.join(resDir, 'drawable/ic_launcher_foreground.xml')), 'ic_launcher_foreground.xml must exist');
  assert.ok(fs.existsSync(path.join(resDir, 'mipmap-anydpi-v26/ic_launcher.xml')), 'adaptive ic_launcher.xml must exist');
  assert.ok(fs.existsSync(path.join(resDir, 'mipmap-anydpi-v26/ic_launcher_round.xml')), 'adaptive ic_launcher_round.xml must exist');
  assert.ok(fs.existsSync(path.join(resDir, 'mipmap-nodpi/ic_launcher.xml')), 'fallback ic_launcher.xml must exist');
  assert.ok(fs.existsSync(path.join(resDir, 'mipmap-nodpi/ic_launcher_round.xml')), 'fallback ic_launcher_round.xml must exist');
});

test('Android Configuration — Assets Bundling Integrity', () => {
  const assetsDir = path.join(appDir, 'src/main/assets/www');
  assert.ok(fs.existsSync(path.join(assetsDir, 'index.html')), 'assets/www/index.html must exist');
  assert.ok(fs.existsSync(path.join(assetsDir, 'style.css')), 'assets/www/style.css must exist');
  assert.ok(fs.existsSync(path.join(assetsDir, 'bundle.js')), 'assets/www/bundle.js must exist');

  const bundleContent = fs.readFileSync(path.join(assetsDir, 'bundle.js'), 'utf8');
  assert.ok(bundleContent.length > 5000, 'bundle.js must contain bundled game modules');
});

test('Android Configuration — GitHub Actions Workflow Integrity', () => {
  const workflowPath = path.join(rootDir, '.github/workflows/android-build.yml');
  assert.ok(fs.existsSync(workflowPath), 'android-build.yml workflow must exist');

  const workflow = fs.readFileSync(workflowPath, 'utf8');
  assert.ok(workflow.includes('gradle/actions/setup-gradle@v3'), 'Must use official setup-gradle action');
  assert.ok(workflow.includes('actions/setup-java@v4'), 'Must setup Java');
  assert.ok(workflow.includes("java-version: '17'"), 'Must use Java 17');
  assert.ok(workflow.includes('actions/setup-node@v4'), 'Must setup Node.js for tests and bundler');
  assert.ok(workflow.includes('assembleRelease'), 'Must build Release APK');
  assert.ok(workflow.includes('actions/upload-artifact@v4'), 'Must upload APK artifact');
});
