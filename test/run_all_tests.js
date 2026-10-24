const { execSync } = require('child_process');
const path = require('path');

const testFiles = [
  'test/navigation.test.js',
  'test/battle.test.js',
  'test/towers.test.js',
  'test/heroes.test.js',
  'test/progression.test.js',
  'test/shop_storage.test.js',
  'test/boss.test.js',
  'test/android_build_validation.test.js'
];

console.log('================================================================');
console.log('    TOWER DEFENSE RPG — MASTER AUTOMATED TEST RUNNER');
console.log('================================================================\n');

let allPassed = true;
const results = [];

for (const file of testFiles) {
  const label = path.basename(file, '.test.js').toUpperCase();
  process.stdout.write(`Executing [${label}] ... `);
  const startTime = Date.now();

  try {
    const output = execSync(`node --test ${file}`, { encoding: 'utf8', stdio: 'pipe' });
    const duration = Date.now() - startTime;
    console.log(`PASSED (${duration}ms)`);
    results.push({ suite: label, status: 'PASSED', duration, output });
  } catch (err) {
    allPassed = false;
    const duration = Date.now() - startTime;
    console.log(`FAILED (${duration}ms)`);
    console.error(err.stdout || err.stderr || err.message);
    results.push({ suite: label, status: 'FAILED', duration, error: err.stdout });
  }
}

console.log('\n================================================================');
console.log('                    TEST EXECUTION SUMMARY');
console.log('================================================================');
for (const r of results) {
  const icon = r.status === 'PASSED' ? '✅' : '❌';
  console.log(`${icon}  ${r.suite.padEnd(20)} : ${r.status} (${r.duration}ms)`);
}
console.log('================================================================');

if (!allPassed) {
  console.error('\n❌ One or more automated test suites failed!');
  process.exit(1);
} else {
  console.log('\n✅ All test suites passed successfully (100% pass rate)!');
  process.exit(0);
}
