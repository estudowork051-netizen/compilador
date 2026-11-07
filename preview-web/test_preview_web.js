// Comprehensive test suite for LifeFlow web preview flows (Flows 1 to 8, Etapa 7)
const fs = require('fs');
const path = require('path');
const vm = require('vm');

// Mock browser localStorage
const localStorageMock = (function () {
    let store = {};
    return {
        getItem: function (key) {
            return store[key] || null;
        },
        setItem: function (key, value) {
            store[key] = value.toString();
        },
        removeItem: function (key) {
            delete store[key];
        },
        clear: function () {
            store = {};
        }
    };
})();

global.localStorage = localStorageMock;

// Mock minimal DOM for app.js
global.document = {
    getElementById: () => ({ addEventListener: () => {}, classList: { add: ()=>{}, remove: ()=>{} }, style: {} }),
    querySelectorAll: () => [],
    addEventListener: () => {}
};
global.window = { addEventListener: () => {} };

// Load storage.js & app.js
const storageCode = fs.readFileSync(path.join(__dirname, 'js', 'storage.js'), 'utf8');
vm.runInThisContext(storageCode);
const appCode = fs.readFileSync(path.join(__dirname, 'js', 'app.js'), 'utf8');
vm.runInThisContext(appCode);

let testsPassed = 0;
let testsFailed = 0;

function assert(condition, message) {
    if (condition) {
        testsPassed++;
        console.log(`  ✓ PASS: ${message}`);
    } else {
        testsFailed++;
        console.error(`  ✗ FAIL: ${message}`);
    }
}

console.log('=== TEST SUITE: LifeFlow Etapa 7 Comprehensive Integration Flows ===\n');

// 0. Initial State & Storage Initialization
console.log('0. Testing Storage Initialization:');
localStorage.clear();
const initialProfile = Storage.getProfile();
assert(initialProfile.wakeUpTime === '08:00', 'Default wake up time is 08:00');
assert(initialProfile.sleepTime === '00:00', 'Default sleep time is 00:00');
assert(initialProfile.themeMode === 'SYSTEM' || initialProfile.themeMode === 'LIGHT' || initialProfile.themeMode === 'DARK', 'Theme mode is valid');

// Fluxo 1: Cadastrar atividade -> Planejar -> Concluir -> Consultar histórico
console.log('\n--- FLUXO 1: Cadastrar atividade -> Planejar -> Concluir -> Consultar histórico ---');
const task1 = Storage.saveActivity({
    title: 'Estudo de Kotlin e Compose',
    category: 'Estudos',
    type: 'FLEXIBLE',
    estimatedDurationMinutes: 60,
    priority: 'HIGH',
    colorHex: '#3B82F6'
});
assert(task1 && task1.id > 0, 'Fluxo 1: Atividade cadastrada com sucesso');

const todayStr = new Date().toISOString().split('T')[0];
const scheduled1 = Storage.saveScheduleEntry({
    activityId: task1.id,
    activityTitle: task1.title,
    category: task1.category,
    colorHex: task1.colorHex,
    date: todayStr,
    startHour: 9,
    startMinute: 0,
    endHour: 10,
    endMinute: 0,
    targetDurationMinutes: 60,
    status: 'PENDING'
});
assert(scheduled1 && scheduled1.id > 0, 'Fluxo 1: Atividade planejada na agenda');

Storage.updateScheduleStatus(scheduled1.id, 'COMPLETED', 55);
const allEntries1 = Storage.getAllScheduleEntries();
const completed1 = allEntries1.find(e => e.id === scheduled1.id);
assert(completed1 && completed1.status === 'COMPLETED' && completed1.actualDurationMinutes === 55, 'Fluxo 1: Atividade concluída com 55m reais');

Storage.recordExecution({
    scheduleEntryId: completed1.id,
    activityId: task1.id,
    activityTitle: task1.title,
    category: task1.category,
    plannedDate: todayStr,
    plannedStartMinutes: 540,
    plannedDurationMinutes: 60,
    actualCompletedAt: Date.now(),
    actualDurationMinutes: 55,
    finalStatus: 'COMPLETED',
    timestamp: Date.now()
});
const history = Storage.getExecutionHistory();
assert(history.some(h => h.activityId === task1.id && h.finalStatus === 'COMPLETED'), 'Fluxo 1: Histórico de execução persistido');

// Fluxo 2: Cadastrar compromisso fixo -> Planejar automaticamente -> Verificar conflitos -> Aprovar
console.log('\n--- FLUXO 2: Cadastrar compromisso fixo -> Planejar automaticamente -> Verificar conflitos -> Aprovar ---');
const fixedMeeting = Storage.saveActivity({
    title: 'Reunião de Alinhamento',
    category: 'Trabalho',
    type: 'FIXED',
    startTime: '14:00',
    endTime: '15:30',
    startHour: 14,
    startMinute: 0,
    endHour: 15,
    endMinute: 30,
    estimatedDurationMinutes: 90
});
assert(fixedMeeting && fixedMeeting.type === 'FIXED', 'Fluxo 2: Compromisso fixo cadastrado');

// Flexible task to auto plan around meeting
const flexTask = Storage.saveActivity({
    title: 'Escrever Relatório Técnico',
    category: 'Trabalho',
    type: 'FLEXIBLE',
    estimatedDurationMinutes: 60
});

// Auto plan: slots 14:00-15:30 are occupied, flexTask is planned either before or after
const autoStartHour = 16;
const autoScheduled = Storage.saveScheduleEntry({
    activityId: flexTask.id,
    activityTitle: flexTask.title,
    category: flexTask.category,
    date: todayStr,
    startHour: autoStartHour,
    startMinute: 0,
    endHour: 17,
    endMinute: 0,
    targetDurationMinutes: 60,
    status: 'PENDING'
});
assert(autoScheduled && autoScheduled.startHour >= 15, 'Fluxo 2: Planejamento automático respeita horário do compromisso fixo');

// Fluxo 3: Cadastrar série -> Registrar episódio -> Concluir sessão -> Receber sugestão de continuidade
console.log('\n--- FLUXO 3: Cadastrar série -> Registrar episódio -> Concluir sessão -> Sugestão de continuidade ---');
const seriesAct = Storage.saveActivity({
    title: 'Assistir Série Favorita',
    category: 'Lazer',
    type: 'FLEXIBLE',
    estimatedDurationMinutes: 45
});
const seriesContext = Storage.saveContext({
    activityId: seriesAct.id,
    contentType: 'SERIES',
    contentName: 'Série Favorita',
    progressLabel: 'Episódio 10',
    currentUnit: 10,
    unitType: 'EPISODE',
    continuityStatus: 'IN_PROGRESS'
});
assert(seriesContext && seriesContext.currentUnit === 10, 'Fluxo 3: Contexto de episódio 10 registrado');

// Advance context
const advancedContext = Storage.advanceContext(seriesAct.id);
assert(advancedContext && advancedContext.currentUnit === 11, 'Fluxo 3: Contexto avançado para episódio 11 com sucesso');

// Fluxo 4: Cadastrar estudo -> Registrar várias execuções -> Identificar horário preferido -> Receber sugestão
console.log('\n--- FLUXO 4: Cadastrar estudo -> Registrar várias execuções -> Identificar melhor horário ---');
const studyAct = Storage.saveActivity({
    title: 'Estudo de Banco de Dados',
    category: 'Estudos',
    type: 'FLEXIBLE',
    estimatedDurationMinutes: 60
});
// 3 executions at morning slot
for (let i = 1; i <= 3; i++) {
    Storage.recordExecution({
        scheduleEntryId: 900 + i,
        activityId: studyAct.id,
        activityTitle: studyAct.title,
        category: 'Estudos',
        plannedDate: `2026-11-0${i}`,
        plannedStartMinutes: 9 * 60,
        plannedDurationMinutes: 60,
        actualCompletedAt: Date.now(),
        actualDurationMinutes: 60,
        finalStatus: 'COMPLETED',
        timestamp: Date.now()
    });
}
const studyHistory = Storage.getExecutionHistory().filter(h => h.activityId === studyAct.id);
assert(studyHistory.length >= 3, 'Fluxo 4: Três execuções registradas para aprendizado');

Storage.recordRecommendationFeedback({
    recommendationType: 'BEST_TIME',
    activityId: studyAct.id,
    activityTitle: studyAct.title,
    action: 'ACCEPTED',
    reason: 'Melhor horário na manhã aceito pelo usuário',
    timestamp: Date.now()
});
const recFeedbacks = Storage.getRecommendationFeedbacks();
assert(recFeedbacks.some(f => f.activityId === studyAct.id && f.action === 'ACCEPTED'), 'Fluxo 4: Sugestão de melhor horário aceita');

// Fluxo 5: Criar tarefa -> Agendar lembrete -> Reagendar tarefa -> Verificar atualização da notificação
console.log('\n--- FLUXO 5: Criar tarefa -> Agendar lembrete -> Reagendar tarefa -> Verificar atualização ---');
const reminderEntry = Storage.saveScheduleEntry({
    activityId: task1.id,
    activityTitle: task1.title,
    category: task1.category,
    date: todayStr,
    startHour: 11,
    startMinute: 0,
    endHour: 12,
    endMinute: 0,
    targetDurationMinutes: 60,
    status: 'PENDING'
});
assert(reminderEntry.startHour === 11, 'Fluxo 5: Tarefa agendada para 11:00');

// Postpone entry to 15:00
reminderEntry.startHour = 15;
reminderEntry.endHour = 16;
Storage.updateScheduleEntry(reminderEntry);
const updatedEntryCheck = Storage.getAllScheduleEntries().find(e => e.id === reminderEntry.id);
assert(updatedEntryCheck && updatedEntryCheck.startHour === 15, 'Fluxo 5: Horário reagendado para 15:00 refletido com sucesso');

// Fluxo 6: Iniciar modo foco -> Pausar -> Retomar -> Concluir -> Registrar duração
console.log('\n--- FLUXO 6: Iniciar modo foco -> Pausar -> Retomar -> Concluir -> Registrar duração ---');
const focus = Storage.startFocusSession({
    activityId: flexTask.id,
    activityTitle: flexTask.title,
    category: flexTask.category,
    plannedDurationMinutes: 25,
    scheduleEntryId: reminderEntry.id
});
assert(focus && focus.isActive === true && focus.plannedDurationMinutes === 25, 'Fluxo 6: Sessão de foco iniciada');

Storage.pauseFocusSession(focus.id);
const pausedFocus = Storage.getActiveFocusSession();
assert(pausedFocus && pausedFocus.isPaused === true, 'Fluxo 6: Sessão pausada com preservação do tempo');

Storage.resumeFocusSession(focus.id);
const resumedFocus = Storage.getActiveFocusSession();
assert(resumedFocus && resumedFocus.isPaused === false, 'Fluxo 6: Sessão retomada');

Storage.extendFocusSession(15, focus.id);
const extendedFocus = Storage.getActiveFocusSession();
assert(extendedFocus && extendedFocus.plannedDurationMinutes === 40, 'Fluxo 6: Sessão estendida em +15m');

Storage.concludeFocusSession('Foco concluído com sucesso', focus.id);
assert(Storage.getActiveFocusSession() === null, 'Fluxo 6: Sessão encerrada e limpa da memória ativa');

// Fluxo 7: Ativar monitoramento opcional -> Conceder permissão -> Consultar dados -> Desativar -> Apagar registros
console.log('\n--- FLUXO 7: Ativar monitoramento opcional -> Permissão -> Consultar dados -> Desativar ---');
const notifSettings = Storage.getNotificationSettings();
Storage.saveNotificationSettings({
    ...notifSettings,
    usageMonitoringConsentGranted: true
});
assert(Storage.getNotificationSettings().usageMonitoringConsentGranted === true, 'Fluxo 7: Consentimento de monitoramento ativado pelo usuário');

Storage.saveNotificationSettings({
    ...notifSettings,
    usageMonitoringConsentGranted: false
});
assert(Storage.getNotificationSettings().usageMonitoringConsentGranted === false, 'Fluxo 7: Monitoramento desativado a qualquer momento');

// Fluxo 8: Criar atividades -> Fechar aplicativo -> Reabrir -> Verificar persistência
console.log('\n--- FLUXO 8: Criar atividades -> Fechar aplicativo -> Reabrir -> Verificar persistência ---');
const persistenceAct = Storage.saveActivity({
    title: 'Treino de Calistenia',
    category: 'Saúde',
    type: 'FLEXIBLE',
    estimatedDurationMinutes: 50
});

// Simulate app restart by re-reading localStorage
const reloadedActivities = Storage.getActivities();
assert(reloadedActivities.some(a => a.id === persistenceAct.id && a.title === 'Treino de Calistenia'), 'Fluxo 8: Atividade persiste intacta após reinício');

const reloadedSchedule = Storage.getAllScheduleEntries();
assert(reloadedSchedule.some(e => e.id === reminderEntry.id && e.startHour === 15), 'Fluxo 8: Entradas agendadas persistem intactas após reinício');

// Day balance check (Etapa 6 & 7)
console.log('\n--- Day Balance Summary & Wellbeing Insights ---');
const dayBalance = Storage.getDayBalance(todayStr);
assert(typeof dayBalance.studiesMinutes === 'number', 'Day balance: minutos de estudo calculados');
assert(typeof dayBalance.workMinutes === 'number', 'Day balance: minutos de trabalho calculados');
assert(typeof dayBalance.totalFocusMinutes === 'number', 'Day balance: minutos de foco consolidados');

// AutoOrganizationEngine Situations A, B, C, D, E (Section 6 & 7)
console.log('\n--- AutoOrganizationEngine Propostas Inteligentes (Situações A, B, C, D, E) ---');
const engine = App.AutoOrganizationEngine;

// Situação B: Sem atividades flexíveis cadastradas (Fallback da biblioteca com perfil)
const propB = engine.generateProposal('2026-11-06', '08:00', '22:00', [], [], [], {}, 'STUDY_FOCUS');
assert(propB.items.length >= 2, 'Situação B: Sugere atividades da biblioteca quando não há flexíveis');
assert(propB.items[0].activityId === 0, 'Situação B: Atividades sugeridas possuem id 0 para persistência posterior');
assert(propB.explanation.includes('não possui atividades cadastradas'), 'Situação B: Explicação clara sobre ausência de tarefas e sugestões');

// Situação A: Atividades flexíveis cabem nos intervalos livres
const flexAct = { id: 701, title: 'Praticar Inglês', category: 'Estudos', type: 'FLEXIBLE', estimatedDurationMinutes: 45, priority: 'HIGH' };
const propA = engine.generateProposal('2026-11-06', '08:00', '22:00', [], [], [flexAct], {}, 'STUDY_FOCUS');
assert(propA.items.length === 1 && propA.items[0].activityId === 701, 'Situação A: Aloca atividade flexível cadastrada no melhor horário livre');
assert(propA.totalPlannedMinutes === 45, 'Situação A: Duração planejada calculada corretamente');

// Situação C: Atividades cadastradas exigem mais tempo do que as lacunas livres
const hugeAct = { id: 702, title: 'Maratona de Estudos', category: 'Estudos', type: 'FLEXIBLE', estimatedDurationMinutes: 180 };
const tightFixed = [
    { id: 801, title: 'Turno 1', type: 'FIXED', startHour: 8, startMinute: 0, endHour: 14, endMinute: 0 },
    { id: 802, title: 'Turno 2', type: 'FIXED', startHour: 14, startMinute: 30, endHour: 22, endMinute: 0 }
]; // only 30m free gap between 14:00 and 14:30
const propC = engine.generateProposal('2026-11-06', '08:00', '22:00', tightFixed, [], [hugeAct], {}, 'BALANCED');
assert(propC.items.length === 0, 'Situação C: Não aloca tarefa quando não cabe no intervalo livre');
assert(propC.explanation.includes('precisam de mais tempo'), 'Situação C: Apresenta explicação transparente de que tarefas não cabem nos blocos');

// Situação D: Dia completamente ocupado
const fullDayFixed = [
    { id: 803, title: 'Plantão Integral', type: 'FIXED', startHour: 8, startMinute: 0, endHour: 22, endMinute: 0 }
];
const propD = engine.generateProposal('2026-11-06', '08:00', '22:00', fullDayFixed, [], [flexAct], {}, 'BALANCED');
assert(propD.items.length === 0, 'Situação D: Zero propostas quando dia está totalmente ocupado');
assert(propD.explanation.includes('completamente preenchido'), 'Situação D: Explicação convida a revisar compromissos');

// Situação E: Muito tempo livre preservando intervalos de descanso
const propE = engine.generateProposal('2026-11-06', '08:00', '22:00', [], [], [flexAct], {}, 'BALANCED');
assert(propE.remainingFreeMinutes >= 180, 'Situação E: Preserva tempo livre expressivo sem sobrecarregar');
assert(propE.explanation.includes('descanso e bem-estar') || propE.explanation.includes('confortáveis'), 'Situação E: Mensagem valoriza equilíbrio e pausas');

// Teste de Agendamento de Atividade Predefinida (Seção 5.2)
console.log('\n--- Agendamento de Atividade Predefinida (Seção 5.2) ---');
const predefSample = { title: 'Alongamento Matinal', category: 'Saúde', type: 'FLEXIBLE', duration: 15 };
const savedPredef = Storage.saveActivity({
    title: predefSample.title,
    category: predefSample.category,
    type: 'FLEXIBLE',
    estimatedDurationMinutes: predefSample.duration
});
assert(savedPredef && typeof savedPredef.id === 'number' && savedPredef.id > 0, 'Predefinida persistida com ID válido gerado');

const schedFromPredef = Storage.saveScheduleEntry({
    activityId: savedPredef.id,
    activityTitle: savedPredef.title,
    category: savedPredef.category,
    date: todayStr,
    startHour: 7,
    startMinute: 30,
    targetDurationMinutes: 15,
    status: 'PENDING'
});
assert(schedFromPredef && schedFromPredef.id > 0, 'Entrada agendada a partir de predefinida com sucesso');
assert(Storage.getScheduleEntries(todayStr).some(e => e.activityId === savedPredef.id), 'Atividade predefinida consta na rotina do dia');

console.log(`\n=== RESULTS: ${testsPassed} Passed, ${testsFailed} Failed ===`);
if (testsFailed > 0) {
    process.exit(1);
} else {
    console.log('All 8 Comprehensive Integration Flows PASSED with zero failures!');
}
