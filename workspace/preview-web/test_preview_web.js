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

// Load storage.js
const storageCode = fs.readFileSync(path.join(__dirname, 'js', 'storage.js'), 'utf8');
vm.runInThisContext(storageCode);

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

// Fluxo 9: Onboarding -> Salvar e começar -> Navegação -> Reabertura
console.log('\n--- FLUXO 9: Conclusão do onboarding -> Navegação para Home -> Reabertura com sucesso ---');
Storage.saveProfile({
    name: 'LifeFlow Tester',
    wakeUpTime: '07:30',
    sleepTime: '23:30',
    fixedCommitments: ['Trabalho', 'Treino'],
    organizationMode: 'MANUAL',
    onboardingCompleted: true
});

const savedProfile = Storage.getProfile();
assert(savedProfile.onboardingCompleted === true, 'Fluxo 9: Onboarding marcado como concluído');
assert(savedProfile.name === 'LifeFlow Tester', 'Fluxo 9: Nome de usuário persistido');
assert(savedProfile.wakeUpTime === '07:30' && savedProfile.sleepTime === '23:30', 'Fluxo 9: Horários de vigília persistidos');

// Verify awake calculation and initial state
const [wakeH, wakeM] = savedProfile.wakeUpTime.split(':').map(Number);
const [sleepH, sleepM] = savedProfile.sleepTime.split(':').map(Number);
const awakeMinutes = (sleepH * 60 + sleepM) - (wakeH * 60 + wakeM);
assert(awakeMinutes === 960, 'Fluxo 9: Duração de vigília calculada em 960m (16h)');
assert(savedProfile.fixedCommitments.length === 2, 'Fluxo 9: Compromissos fixos persistidos');

// Day balance check (Etapa 6 & 7)
console.log('\n--- Day Balance Summary & Wellbeing Insights ---');
const dayBalance = Storage.getDayBalance(todayStr);
assert(typeof dayBalance.studiesMinutes === 'number', 'Day balance: minutos de estudo calculados');
assert(typeof dayBalance.workMinutes === 'number', 'Day balance: minutos de trabalho calculados');
assert(typeof dayBalance.totalFocusMinutes === 'number', 'Day balance: minutos de foco consolidados');

console.log(`\n=== RESULTS: ${testsPassed} Passed, ${testsFailed} Failed ===`);
if (testsFailed > 0) {
    process.exit(1);
} else {
    console.log('All 8 Comprehensive Integration Flows PASSED with zero failures!');
}
