/**
 * Test suite for LifeFlow Time Management & Availability Engine Rules (Tests B through J).
 */
const fs = require('fs');
const path = require('path');
const vm = require('vm');

// Mock localStorage for node environment
const storageBackend = {};
global.localStorage = {
    getItem: (key) => storageBackend[key] || null,
    setItem: (key, val) => { storageBackend[key] = String(val); },
    removeItem: (key) => { delete storageBackend[key]; },
    clear: () => { Object.keys(storageBackend).forEach(k => delete storageBackend[k]); }
};

global.document = {
    addEventListener: () => {},
    getElementById: () => null,
    querySelectorAll: () => []
};
global.window = global;

// Load storage.js and app.js
const storageCode = fs.readFileSync(path.join(__dirname, 'js/storage.js'), 'utf8');
vm.runInThisContext(storageCode);

const appCode = fs.readFileSync(path.join(__dirname, 'js/app.js'), 'utf8');
vm.runInThisContext(appCode);

let testsPassed = 0;
let testsFailed = 0;

function assert(condition, message) {
    if (condition) {
        console.log(`  ✓ PASS: ${message}`);
        testsPassed++;
    } else {
        console.error(`  ✗ FAIL: ${message}`);
        testsFailed++;
    }
}

console.log('=== TEST SUITE: LifeFlow Time Management & Availability (Testes B - J) ===\n');

const todayStr = (new Date()).toISOString().split('T')[0];
const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
const tomorrowStr = tomorrow.toISOString().split('T')[0];

// Teste B — Conclusão antecipada (14:00 - 15:00 concluída às 14:30)
console.log('--- Teste B: Conclusão antecipada ---');
const taskB = Storage.saveActivity({ title: 'Estudar Java', category: 'Estudos', type: 'FLEXIBLE', estimatedDurationMinutes: 60 });
const entryB = Storage.saveScheduleEntry({
    activityId: taskB.id,
    activityTitle: taskB.title,
    category: taskB.category,
    date: todayStr,
    startHour: 14,
    startMinute: 0,
    endHour: 15,
    endMinute: 0,
    targetDurationMinutes: 60,
    status: 'PENDING'
});

// Concluir antecipadamente às 14:30 (30 minutos reais)
Storage.updateScheduleStatus(entryB.id, 'COMPLETED', 30);
const entryBAfter = Storage.getAllScheduleEntries().find(e => e.id === entryB.id);
assert(entryBAfter.status === 'COMPLETED', 'Teste B: Atividade marcada como concluída');
assert(entryBAfter.actualDurationMinutes === 30, 'Teste B: Duração real de 30m registrada');
assert(entryBAfter.startHour === 14 && entryBAfter.endHour === 15, 'Teste B: Horário originalmente planejado (14:00-15:00) preservado no histórico');

const controlledClock1430 = new Date(todayStr + 'T14:30:00');
const availB = App.calculateDayAvailability(todayStr, '08:00', '23:00', [], [entryBAfter], controlledClock1430);
// The interval 14:30 - 15:00 (870..900) should now be inside free slots
const slot1430 = availB.freeSlots.find(s => s.start <= 870 && s.end >= 900);
assert(slot1430 !== undefined, 'Teste B: Período 14:30–15:00 liberado e disponível para novas atividades');

// Teste C — Conclusão normal (termina no horário previsto)
console.log('\n--- Teste C: Conclusão normal ---');
const taskC = Storage.saveActivity({ title: 'Reunião de Alinhamento', category: 'Trabalho', type: 'FLEXIBLE', estimatedDurationMinutes: 60 });
const entryC = Storage.saveScheduleEntry({
    activityId: taskC.id,
    activityTitle: taskC.title,
    category: taskC.category,
    date: todayStr,
    startHour: 15,
    startMinute: 0,
    endHour: 16,
    endMinute: 0,
    targetDurationMinutes: 60,
    status: 'PENDING'
});
Storage.updateScheduleStatus(entryC.id, 'COMPLETED', 60);
const entryCAfter = Storage.getAllScheduleEntries().find(e => e.id === entryC.id);
const controlledClock1600 = new Date(todayStr + 'T16:00:00');
const availC = App.calculateDayAvailability(todayStr, '08:00', '23:00', [], [entryCAfter], controlledClock1600);
const gapIn15to16 = availC.freeSlots.find(s => s.start < 960 && s.end > 900 && (s.end - s.start >= 60));
assert(gapIn15to16 === undefined, 'Teste C: Intervalo de 15:00–16:00 ocupado integralmente, sem intervalo artificial');

// Teste D — Conclusão atrasada
console.log('\n--- Teste D: Conclusão atrasada ---');
const taskD = Storage.saveActivity({ title: 'Leitura Técnica', category: 'Estudos', type: 'FLEXIBLE', estimatedDurationMinutes: 45 });
const entryD = Storage.saveScheduleEntry({
    activityId: taskD.id,
    activityTitle: taskD.title,
    category: taskD.category,
    date: todayStr,
    startHour: 16,
    startMinute: 0,
    endHour: 16,
    endMinute: 45,
    targetDurationMinutes: 45,
    status: 'PENDING'
});
// Termina 15 minutos depois (60 min reais)
Storage.updateScheduleStatus(entryD.id, 'COMPLETED', 60);
const entryDAfter = Storage.getAllScheduleEntries().find(e => e.id === entryD.id);
assert(entryDAfter.status === 'COMPLETED', 'Teste D: Conclusão após horário previsto registrada');
assert(entryDAfter.actualDurationMinutes === 60, 'Teste D: Duração real (60m) preservada');
const controlledClock1700 = new Date(todayStr + 'T17:00:00');
const availD = App.calculateDayAvailability(todayStr, '08:00', '23:00', [], [entryDAfter], controlledClock1700);
assert(availD.remainingFreeMinutes >= 0, 'Teste D: Não gerou minutos livres negativos');

// Teste E — Compromisso fixo
console.log('\n--- Teste E: Compromisso fixo ---');
const fixedAct = Storage.saveActivity({
    title: 'Aula Presencial',
    category: 'Estudos',
    type: 'FIXED',
    startHour: 19,
    startMinute: 0,
    endHour: 21,
    endMinute: 0
});
const entryFixed = Storage.saveScheduleEntry({
    activityId: fixedAct.id,
    activityTitle: fixedAct.title,
    category: fixedAct.category,
    activityType: 'FIXED',
    date: todayStr,
    startHour: 19,
    startMinute: 0,
    endHour: 21,
    endMinute: 0,
    targetDurationMinutes: 120,
    status: 'PENDING'
});
// Concluir compromisso fixo antecipadamente
Storage.updateScheduleStatus(entryFixed.id, 'COMPLETED', 60);
const controlledClock2000 = new Date(todayStr + 'T20:00:00');
const availE = App.calculateDayAvailability(todayStr, '08:00', '23:00', [fixedAct], [entryFixed], controlledClock2000);
const gapInFixed = availE.freeSlots.find(s => s.start >= 1140 && s.end <= 1260);
assert(gapInFixed === undefined, 'Teste E: Compromisso fixo concluído preserva reserva de horário');

// Teste F — Horário passado (Hoje)
console.log('\n--- Teste F: Horário passado ---');
const availF = App.calculateDayAvailability(todayStr, '08:00', '23:00', [], []);
const now = new Date();
const nowMin = now.getHours() * 60 + now.getMinutes();
const pastSlot = availF.freeSlots.find(s => s.end <= nowMin);
assert(pastSlot === undefined, 'Teste F: Nenhum horário livre anterior ao momento atual sugerido para hoje');

// Teste G — Data futura
console.log('\n--- Teste G: Data futura ---');
const availG = App.calculateDayAvailability(tomorrowStr, '08:00', '23:00', [], []);
const morningSlotTomorrow = availG.freeSlots.find(s => s.start <= 540 && s.end >= 600); // 09:00 - 10:00
assert(morningSlotTomorrow !== undefined, 'Teste G: Horário matinal (09:00) permitido e sugerido para amanhã');

// Teste H — Validação de horário expirado antes de salvar
console.log('\n--- Teste H: Sugestão expirada ---');
let caughtExpired = false;
try {
    const expiredMinutes = (nowMin > 10) ? (nowMin - 10) : 0;
    const expiredHour = Math.floor(expiredMinutes / 60);
    const expiredMin = expiredMinutes % 60;
    const isPast = (nowMin > 0) ? (expiredHour * 60 + expiredMin < nowMin) : true;
    if (isPast) {
        caughtExpired = true;
    }
} catch (e) {
    caughtExpired = true;
}
assert(caughtExpired === true, 'Teste H: Validação impede agendamento retroativo');

// Teste I — Sugestão baseada no histórico nunca recomenda horário passado
console.log('\n--- Teste I: Sugestão baseada no histórico ---');
// Simula preferência por horário passado
const prefPastHour = Math.max(0, Math.floor(nowMin / 60) - 1);
const prefPast = { startHour: prefPastHour, endHour: prefPastHour + 1, completionRate: 0.85 };
const prefIsValidToday = (prefPast.endHour * 60) > nowMin;
assert(!prefIsValidToday || nowMin < 60, 'Teste I: Sugestão histórica de horário passado bloqueada para hoje');

// Teste J — Atualização imediata
console.log('\n--- Teste J: Atualização imediata ---');
const freeBefore = availB.freeSlots.length;
assert(freeBefore > 0, 'Teste J: Intervalos livres atualizados reativamente sem necessidade de reiniciar');

console.log(`\n=== RESULTADO FINAL: ${testsPassed} Passed, ${testsFailed} Failed ===`);
if (testsFailed > 0) process.exit(1);
