/**
 * LifeFlow Web Preview - Storage Manager
 * Uses localStorage to persist user preferences, activities, schedule entries, execution records,
 * contexts, and recommendation feedback (Etapa 5).
 */
const Storage = (() => {
    const PREF_KEY = 'lifeflow_user_preferences';
    const ACTIVITIES_KEY = 'lifeflow_activities';
    const SCHEDULE_KEY = 'lifeflow_schedule_entries';
    const EXECUTION_KEY = 'lifeflow_execution_records';
    const CONTEXTS_KEY = 'lifeflow_activity_contexts';
    const FEEDBACK_KEY = 'lifeflow_recommendation_feedbacks';
    const LEARNING_SETTINGS_KEY = 'lifeflow_learning_settings';

    const defaultProfile = {
        name: '',
        wakeUpTime: '08:00',
        sleepTime: '00:00',
        fixedCommitments: [],
        themeMode: 'SYSTEM', // 'SYSTEM' | 'LIGHT' | 'DARK'
        organizationMode: 'MANUAL', // 'MANUAL' | 'AUTOMATIC'
        balancePreference: 'BALANCED', // 'BALANCED' | 'STUDY_FOCUS' | 'REST_FOCUS' | 'NO_PREFERENCE'
        onboardingCompleted: false,
        onboardingStep: 1
    };

    const defaultLearningSettings = {
        learningEnabled: true,
        personalizedRecommendationsEnabled: true,
        showExplanationsEnabled: true
    };

    function getProfile() {
        try {
            const raw = localStorage.getItem(PREF_KEY);
            if (!raw) return { ...defaultProfile };
            const parsed = JSON.parse(raw);
            return { ...defaultProfile, ...parsed };
        } catch (e) {
            console.error('Error loading preferences from localStorage:', e);
            return { ...defaultProfile };
        }
    }

    function saveProfile(profile) {
        try {
            localStorage.setItem(PREF_KEY, JSON.stringify(profile));
        } catch (e) {
            console.error('Error saving preferences to localStorage:', e);
        }
    }

    function updateField(key, value) {
        const current = getProfile();
        current[key] = value;
        saveProfile(current);
        return current;
    }

    // ==========================================
    // Learning Settings (Etapa 5)
    // ==========================================
    function getLearningSettings() {
        try {
            const raw = localStorage.getItem(LEARNING_SETTINGS_KEY);
            if (!raw) return { ...defaultLearningSettings };
            return { ...defaultLearningSettings, ...JSON.parse(raw) };
        } catch (e) {
            return { ...defaultLearningSettings };
        }
    }

    function saveLearningSettings(settings) {
        try {
            localStorage.setItem(LEARNING_SETTINGS_KEY, JSON.stringify(settings));
        } catch (e) {
            console.error('Error saving learning settings:', e);
        }
    }

    // ==========================================
    // Activities CRUD
    // ==========================================
    function getActivities() {
        try {
            const raw = localStorage.getItem(ACTIVITIES_KEY);
            if (!raw) {
                const initialSeed = seedInitialActivities();
                saveActivities(initialSeed);
                return initialSeed;
            }
            return JSON.parse(raw);
        } catch (e) {
            console.error('Error loading activities from localStorage:', e);
            return [];
        }
    }

    function saveActivities(list) {
        try {
            localStorage.setItem(ACTIVITIES_KEY, JSON.stringify(list));
        } catch (e) {
            console.error('Error saving activities to localStorage:', e);
        }
    }

    function saveActivity(activity) {
        const list = getActivities();
        if (activity.id && activity.id > 0) {
            const idx = list.findIndex(a => a.id === activity.id);
            if (idx >= 0) {
                list[idx] = { ...activity, updatedAt: Date.now() };
            } else {
                list.push({ ...activity, updatedAt: Date.now() });
            }
        } else {
            activity.id = Date.now();
            activity.createdAt = Date.now();
            activity.updatedAt = Date.now();
            list.unshift(activity);
        }
        saveActivities(list);
        return activity;
    }

    function deleteActivity(id) {
        const list = getActivities().filter(a => a.id !== id);
        saveActivities(list);
        const schedules = getAllScheduleEntries().filter(s => s.activityId !== id);
        saveAllScheduleEntries(schedules);
    }

    function duplicateActivity(id) {
        const list = getActivities();
        const existing = list.find(a => a.id === id);
        if (existing) {
            const copy = {
                ...existing,
                id: Date.now(),
                title: `${existing.title} (Cópia)`,
                createdAt: Date.now(),
                updatedAt: Date.now()
            };
            list.unshift(copy);
            saveActivities(list);
            return copy;
        }
        return null;
    }

    // ==========================================
    // Schedule Entries CRUD
    // ==========================================
    function getAllScheduleEntries() {
        try {
            const raw = localStorage.getItem(SCHEDULE_KEY);
            if (!raw) {
                const initial = seedInitialSchedule();
                saveAllScheduleEntries(initial);
                return initial;
            }
            return JSON.parse(raw);
        } catch (e) {
            console.error('Error loading schedule entries:', e);
            return [];
        }
    }

    function saveAllScheduleEntries(list) {
        try {
            localStorage.setItem(SCHEDULE_KEY, JSON.stringify(list));
        } catch (e) {
            console.error('Error saving schedule entries:', e);
        }
    }

    function getScheduleEntries(dateStr) {
        const all = getAllScheduleEntries();
        return all.filter(e => e.date === dateStr).sort((a, b) => {
            const sA = a.startHour * 60 + a.startMinute;
            const sB = b.startHour * 60 + b.startMinute;
            return sA - sB;
        });
    }

    function saveScheduleEntry(entry) {
        const list = getAllScheduleEntries();
        if (entry.id && entry.id > 0) {
            const idx = list.findIndex(e => e.id === entry.id);
            if (idx >= 0) {
                list[idx] = entry;
            } else {
                list.push(entry);
            }
        } else {
            entry.id = Date.now();
            if (!entry.status) entry.status = 'PENDING';
            list.push(entry);
        }
        saveAllScheduleEntries(list);
        return entry;
    }

    function deleteScheduleEntry(id) {
        const list = getAllScheduleEntries().filter(e => e.id !== id);
        saveAllScheduleEntries(list);
    }

    function updateScheduleEntry(updated) {
        const list = getAllScheduleEntries();
        const idx = list.findIndex(e => e.id === updated.id);
        if (idx !== -1) {
            list[idx] = { ...list[idx], ...updated };
            saveAllScheduleEntries(list);
        }
    }

    function updateScheduleStatus(id, newStatus, actualDurationMinutes = null, notes = '') {
        const list = getAllScheduleEntries();
        const entry = list.find(e => e.id === id);
        if (entry) {
            entry.status = newStatus;
            if (actualDurationMinutes !== null) entry.actualDurationMinutes = actualDurationMinutes;
            if (notes) entry.notes = notes;
            if (newStatus === 'COMPLETED') entry.completedAt = Date.now();
            saveAllScheduleEntries(list);

            recordExecution({
                scheduleEntryId: entry.id,
                activityId: entry.activityId,
                activityTitle: entry.activityTitle,
                category: entry.category,
                plannedDate: entry.date,
                plannedStartMinutes: entry.startHour * 60 + entry.startMinute,
                plannedDurationMinutes: entry.targetDurationMinutes,
                actualCompletedAt: entry.completedAt,
                actualDurationMinutes: actualDurationMinutes,
                finalStatus: newStatus,
                timestamp: Date.now(),
                notes: notes
            });
        }
    }

    function postponeScheduleEntry(id, newDateStr) {
        const list = getAllScheduleEntries();
        const entry = list.find(e => e.id === id);
        if (entry) {
            const oldDate = entry.date;
            entry.date = newDateStr;
            entry.postponedToDate = newDateStr;
            entry.status = 'PENDING';
            saveAllScheduleEntries(list);

            recordExecution({
                scheduleEntryId: entry.id,
                activityId: entry.activityId,
                activityTitle: entry.activityTitle,
                category: entry.category,
                plannedDate: oldDate,
                plannedStartMinutes: entry.startHour * 60 + entry.startMinute,
                plannedDurationMinutes: entry.targetDurationMinutes,
                actualCompletedAt: null,
                actualDurationMinutes: null,
                finalStatus: 'POSTPONED',
                timestamp: Date.now(),
                notes: `Adiada para ${newDateStr}`
            });
        }
    }

    // ==========================================
    // Execution History (Etapa 3 / 5)
    // ==========================================
    function getExecutionHistory() {
        try {
            const raw = localStorage.getItem(EXECUTION_KEY);
            if (!raw) return [];
            return JSON.parse(raw);
        } catch (e) {
            console.error('Error loading execution history:', e);
            return [];
        }
    }

    function recordExecution(record) {
        const history = getExecutionHistory();
        record.id = Date.now() + Math.floor(Math.random() * 1000);
        history.unshift(record);
        try {
            localStorage.setItem(EXECUTION_KEY, JSON.stringify(history));
        } catch (e) {
            console.error('Error saving execution record:', e);
        }
    }

    // ==========================================
    // Recommendation Feedback (Etapa 5)
    // ==========================================
    function getRecommendationFeedbacks() {
        try {
            const raw = localStorage.getItem(FEEDBACK_KEY);
            if (!raw) return [];
            return JSON.parse(raw);
        } catch (e) {
            return [];
        }
    }

    function recordRecommendationFeedback(feedback) {
        const list = getRecommendationFeedbacks();
        feedback.id = Date.now();
        feedback.timestamp = Date.now();
        list.unshift(feedback);
        try {
            localStorage.setItem(FEEDBACK_KEY, JSON.stringify(list));
        } catch (e) {
            console.error('Error saving feedback:', e);
        }
    }

    /**
     * Clears learning history and feedback, preserving activities and schedules (Section 14).
     */
    function clearLearningData() {
        try {
            localStorage.removeItem(EXECUTION_KEY);
            localStorage.removeItem(FEEDBACK_KEY);
        } catch (e) {
            console.error('Error clearing learning data:', e);
        }
    }

    // ==========================================
    // Contexts CRUD (Etapa 4 / 5)
    // ==========================================
    function getContexts() {
        try {
            const raw = localStorage.getItem(CONTEXTS_KEY);
            if (!raw) {
                const seed = seedInitialContexts();
                saveContexts(seed);
                return seed;
            }
            return JSON.parse(raw);
        } catch (e) {
            return [];
        }
    }

    function saveContexts(list) {
        try {
            localStorage.setItem(CONTEXTS_KEY, JSON.stringify(list));
        } catch (e) {
            console.error('Error saving contexts:', e);
        }
    }

    function getContextForActivity(activityId) {
        const contexts = getContexts();
        return contexts.find(c => c.activityId == activityId) || null;
    }

    function saveContext(context) {
        const contexts = getContexts();
        const idx = contexts.findIndex(c => c.activityId == context.activityId);
        if (idx >= 0) {
            contexts[idx] = { ...contexts[idx], ...context, updatedAt: Date.now() };
        } else {
            contexts.push({
                id: Date.now(),
                ...context,
                createdAt: Date.now(),
                updatedAt: Date.now()
            });
        }
        saveContexts(contexts);
        return context;
    }

    function advanceContext(activityId) {
        const ctx = getContextForActivity(activityId);
        if (!ctx) return null;
        const currentUnit = ctx.currentUnit || 1;
        const nextUnit = currentUnit + 1;
        let nextLabel = ctx.progressLabel || '';

        if (ctx.contentType === 'SERIES') {
            nextLabel = `Episódio ${nextUnit}`;
        } else if (ctx.contentType === 'BOOK') {
            nextLabel = `Capítulo ${nextUnit}`;
        } else if (ctx.contentType === 'STUDY_COURSE') {
            nextLabel = `Módulo ${nextUnit}`;
        }

        ctx.currentUnit = nextUnit;
        ctx.progressLabel = nextLabel;
        ctx.continuityStatus = 'IN_PROGRESS';
        saveContext(ctx);
        return ctx;
    }

    // ==========================================
    // Demonstration Scenario (Section 17)
    // ==========================================
    function seedDemonstrationHistory() {
        const demoRecords = [
            // Estudo de Programação (id: 2) -> 7 completions at 10h (600m), real duration ~50m
            { id: 1001, activityId: 2, activityTitle: 'Estudos de Programação', category: 'Estudos', plannedStartMinutes: 600, plannedDurationMinutes: 90, actualDurationMinutes: 50, finalStatus: 'COMPLETED', timestamp: Date.now() - 86400000 * 5 },
            { id: 1002, activityId: 2, activityTitle: 'Estudos de Programação', category: 'Estudos', plannedStartMinutes: 600, plannedDurationMinutes: 90, actualDurationMinutes: 50, finalStatus: 'COMPLETED', timestamp: Date.now() - 86400000 * 4 },
            { id: 1003, activityId: 2, activityTitle: 'Estudos de Programação', category: 'Estudos', plannedStartMinutes: 600, plannedDurationMinutes: 90, actualDurationMinutes: 55, finalStatus: 'COMPLETED', timestamp: Date.now() - 86400000 * 3 },
            { id: 1004, activityId: 2, activityTitle: 'Estudos de Programação', category: 'Estudos', plannedStartMinutes: 600, plannedDurationMinutes: 90, actualDurationMinutes: 45, finalStatus: 'COMPLETED', timestamp: Date.now() - 86400000 * 2 },
            { id: 1005, activityId: 2, activityTitle: 'Estudos de Programação', category: 'Estudos', plannedStartMinutes: 600, plannedDurationMinutes: 90, actualDurationMinutes: 50, finalStatus: 'COMPLETED', timestamp: Date.now() - 86400000 * 1 },
            // Lavar Louça (id: 4) -> 4 completions with median 25m (planned 30m)
            { id: 1006, activityId: 4, activityTitle: 'Lavar louça e arrumar cozinha', category: 'Casa', plannedStartMinutes: 780, plannedDurationMinutes: 30, actualDurationMinutes: 20, finalStatus: 'COMPLETED', timestamp: Date.now() - 86400000 * 4 },
            { id: 1007, activityId: 4, activityTitle: 'Lavar louça e arrumar cozinha', category: 'Casa', plannedStartMinutes: 780, plannedDurationMinutes: 30, actualDurationMinutes: 25, finalStatus: 'COMPLETED', timestamp: Date.now() - 86400000 * 3 },
            { id: 1008, activityId: 4, activityTitle: 'Lavar louça e arrumar cozinha', category: 'Casa', plannedStartMinutes: 780, plannedDurationMinutes: 30, actualDurationMinutes: 25, finalStatus: 'COMPLETED', timestamp: Date.now() - 86400000 * 2 },
            // Treino / Academia (id: 3) -> 3 postponements
            { id: 1009, activityId: 3, activityTitle: 'Treino / Academia', category: 'Saúde', plannedStartMinutes: 480, plannedDurationMinutes: 60, actualDurationMinutes: null, finalStatus: 'POSTPONED', timestamp: Date.now() - 86400000 * 3 },
            { id: 1010, activityId: 3, activityTitle: 'Treino / Academia', category: 'Saúde', plannedStartMinutes: 480, plannedDurationMinutes: 60, actualDurationMinutes: null, finalStatus: 'POSTPONED', timestamp: Date.now() - 86400000 * 2 },
            { id: 1011, activityId: 3, activityTitle: 'Treino / Academia', category: 'Saúde', plannedStartMinutes: 480, plannedDurationMinutes: 60, actualDurationMinutes: null, finalStatus: 'POSTPONED', timestamp: Date.now() - 86400000 * 1 }
        ];
        try {
            localStorage.setItem(EXECUTION_KEY, JSON.stringify(demoRecords));
        } catch (e) {
            console.error('Error saving demo history:', e);
        }
        return demoRecords;
    }

    function seedInitialActivities() {
        return [
            {
                id: 1,
                title: 'Trabalho / Estágio',
                category: 'Trabalho',
                estimatedDurationMinutes: 360,
                type: 'FIXED',
                startHour: 16,
                startMinute: 0,
                endHour: 22,
                endMinute: 0,
                recurrenceType: 'WEEKDAYS',
                recurrenceDays: [1, 2, 3, 4, 5],
                colorHex: '#8B5CF6'
            },
            {
                id: 2,
                title: 'Estudos de Programação',
                category: 'Estudos',
                estimatedDurationMinutes: 90,
                type: 'FLEXIBLE',
                defaultCount: 1,
                colorHex: '#3B82F6'
            },
            {
                id: 3,
                title: 'Treino / Academia',
                category: 'Saúde',
                estimatedDurationMinutes: 60,
                type: 'FLEXIBLE',
                defaultCount: 1,
                colorHex: '#10B981'
            },
            {
                id: 4,
                title: 'Lavar louça e arrumar cozinha',
                category: 'Casa',
                estimatedDurationMinutes: 30,
                type: 'FLEXIBLE',
                defaultCount: 1,
                colorHex: '#F59E0B'
            },
            {
                id: 5,
                title: 'Leitura de livro',
                category: 'Lazer',
                estimatedDurationMinutes: 45,
                type: 'FLEXIBLE',
                defaultCount: 1,
                colorHex: '#EC4899'
            }
        ];
    }

    function seedInitialSchedule() {
        const todayStr = new Date().toISOString().split('T')[0];
        return [
            {
                id: 101,
                activityId: 2,
                activityTitle: 'Estudos de Programação',
                category: 'Estudos',
                date: todayStr,
                startHour: 9,
                startMinute: 0,
                endHour: 10,
                endMinute: 30,
                targetDurationMinutes: 90,
                status: 'COMPLETED'
            },
            {
                id: 102,
                activityId: 3,
                activityTitle: 'Treino / Academia',
                category: 'Saúde',
                date: todayStr,
                startHour: 11,
                startMinute: 0,
                endHour: 12,
                endMinute: 0,
                targetDurationMinutes: 60,
                status: 'PENDING'
            },
            {
                id: 103,
                activityId: 4,
                activityTitle: 'Lavar louça e arrumar cozinha',
                category: 'Casa',
                date: todayStr,
                startHour: 13,
                startMinute: 0,
                endHour: 13,
                endMinute: 30,
                targetDurationMinutes: 30,
                status: 'PENDING'
            }
        ];
    }

    function seedInitialContexts() {
        return [
            {
                id: 1,
                activityId: 3,
                contentType: 'SERIES',
                contentName: 'Breaking Bad',
                progressLabel: 'Episódio 35',
                currentUnit: 35,
                unitType: 'EPISODE',
                continuityStatus: 'IN_PROGRESS',
                createdAt: Date.now(),
                updatedAt: Date.now()
            },
            {
                id: 2,
                activityId: 5,
                contentType: 'BOOK',
                contentName: 'O Poder do Hábito',
                progressLabel: 'Capítulo 3',
                currentUnit: 3,
                unitType: 'CHAPTER',
                continuityStatus: 'IN_PROGRESS',
                createdAt: Date.now(),
                updatedAt: Date.now()
            }
        ];
    }

    const FOCUS_KEY = 'lifeflow_focus_sessions';
    const APP_CLASS_KEY = 'lifeflow_app_classifications';
    const APP_USAGE_KEY = 'lifeflow_app_usage_logs';
    const NOTIF_SETTINGS_KEY = 'lifeflow_notification_settings';

    const defaultNotifSettings = {
        remindersEnabled: true,
        notifyBeforeStart: true,
        notifyBeforeMinutes: 15,
        promptCompletionOnEnd: true,
        remindPendingTasks: true,
        reorganizeSuggestionsEnabled: true,
        quietHoursEnabled: true,
        quietHoursStart: '22:00',
        quietHoursEnd: '07:00',
        usageMonitoringConsentGranted: false
    };

    function getNotificationSettings() {
        try {
            const raw = localStorage.getItem(NOTIF_SETTINGS_KEY);
            if (!raw) return { ...defaultNotifSettings };
            return { ...defaultNotifSettings, ...JSON.parse(raw) };
        } catch (e) {
            return { ...defaultNotifSettings };
        }
    }

    function saveNotificationSettings(settings) {
        try {
            localStorage.setItem(NOTIF_SETTINGS_KEY, JSON.stringify(settings));
        } catch (e) {
            console.error('Error saving notification settings:', e);
        }
    }

    function getFocusSessions() {
        try {
            const raw = localStorage.getItem(FOCUS_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];
        }
    }

    function saveFocusSessions(list) {
        try {
            localStorage.setItem(FOCUS_KEY, JSON.stringify(list));
        } catch (e) {
            console.error('Error saving focus sessions:', e);
        }
    }

    function getActiveFocusSession() {
        const list = getFocusSessions();
        return list.find(s => s.isActive) || null;
    }

    function startFocusSession({ activityId, activityTitle, category, plannedDurationMinutes, scheduleEntryId }) {
        const list = getFocusSessions();
        list.forEach(s => { if (s.isActive) s.isActive = false; });
        const newSession = {
            id: Date.now(),
            scheduleEntryId: scheduleEntryId || null,
            activityId: activityId || 0,
            activityTitle: activityTitle || 'Momento de Foco',
            category: category || 'Outros',
            plannedDurationMinutes: plannedDurationMinutes || 25,
            startTimeMillis: Date.now(),
            elapsedSecondsBeforePause: 0,
            pausedAtMillis: null,
            isPaused: false,
            isActive: true,
            completedAtMillis: null,
            notes: ''
        };
        list.push(newSession);
        saveFocusSessions(list);
        return newSession;
    }

    function pauseFocusSession(id) {
        const list = getFocusSessions();
        const session = list.find(s => (id ? s.id === id : s.isActive));
        if (session && !session.isPaused) {
            const now = Date.now();
            const elapsed = Math.floor((now - session.startTimeMillis) / 1000);
            session.elapsedSecondsBeforePause += Math.max(0, elapsed);
            session.isPaused = true;
            session.pausedAtMillis = now;
            saveFocusSessions(list);
        }
        return session;
    }

    function resumeFocusSession(id) {
        const list = getFocusSessions();
        const session = list.find(s => (id ? s.id === id : s.isActive));
        if (session && session.isPaused) {
            session.isPaused = false;
            session.pausedAtMillis = null;
            session.startTimeMillis = Date.now();
            saveFocusSessions(list);
        }
        return session;
    }

    function extendFocusSession(additionalMinutes, id) {
        const list = getFocusSessions();
        const session = list.find(s => (id ? s.id === id : s.isActive));
        if (session) {
            session.plannedDurationMinutes += (additionalMinutes || 15);
            saveFocusSessions(list);
        }
        return session;
    }

    function concludeFocusSession(notes, id) {
        const list = getFocusSessions();
        const session = list.find(s => (id ? s.id === id : s.isActive));
        if (session) {
            const now = Date.now();
            let totalElapsed = session.elapsedSecondsBeforePause;
            if (!session.isPaused) {
                totalElapsed += Math.max(0, Math.floor((now - session.startTimeMillis) / 1000));
            }
            session.isActive = false;
            session.isPaused = false;
            session.completedAtMillis = now;
            session.notes = notes || '';
            saveFocusSessions(list);

            if (session.scheduleEntryId) {
                const actualMin = Math.max(1, Math.round(totalElapsed / 60));
                updateScheduleStatus(session.scheduleEntryId, 'COMPLETED', actualMin);
            }
        }
        return session;
    }

    function getDayBalance(dateStr) {
        const entries = getScheduleEntries(dateStr);
        let studies = 0, work = 0, leisure = 0, home = 0, rest = 0, others = 0;
        let completed = 0, postponed = 0, pending = 0;

        entries.forEach(e => {
            const dur = e.targetDurationMinutes || 0;
            const cat = (e.category || '').toLowerCase();
            if (cat.includes('estud')) studies += dur;
            else if (cat.includes('trabalh')) work += dur;
            else if (cat.includes('lazer')) leisure += dur;
            else if (cat.includes('casa')) home += dur;
            else if (cat.includes('descans')) rest += dur;
            else others += dur;

            if (e.status === 'COMPLETED') completed++;
            else if (e.status === 'POSTPONED') postponed++;
            else pending++;
        });

        const focusSessions = getFocusSessions();
        let totalFocusMinutes = 0;
        focusSessions.forEach(s => {
            let elapsedSec = s.elapsedSecondsBeforePause || 0;
            if (s.isActive && !s.isPaused) {
                elapsedSec += Math.max(0, Math.floor((Date.now() - s.startTimeMillis) / 1000));
            }
            totalFocusMinutes += Math.round(elapsedSec / 60);
        });

        return {
            studiesMinutes: studies,
            workMinutes: work,
            leisureMinutes: leisure,
            homeMinutes: home,
            restMinutes: rest,
            otherMinutes: others,
            totalFocusMinutes,
            completedTasksCount: completed,
            postponedTasksCount: postponed,
            pendingTasksCount: pending
        };
    }

    function resetToDefault() {
        try {
            localStorage.removeItem(PREF_KEY);
            localStorage.removeItem(ACTIVITIES_KEY);
            localStorage.removeItem(SCHEDULE_KEY);
            localStorage.removeItem(EXECUTION_KEY);
            localStorage.removeItem(CONTEXTS_KEY);
            localStorage.removeItem(FEEDBACK_KEY);
            localStorage.removeItem(LEARNING_SETTINGS_KEY);
            localStorage.removeItem(FOCUS_KEY);
            localStorage.removeItem(APP_CLASS_KEY);
            localStorage.removeItem(APP_USAGE_KEY);
            localStorage.removeItem(NOTIF_SETTINGS_KEY);
        } catch (e) {
            console.error('Error clearing localStorage:', e);
        }
        return { ...defaultProfile };
    }

    return {
        getProfile,
        saveProfile,
        updateField,
        getLearningSettings,
        saveLearningSettings,
        getNotificationSettings,
        saveNotificationSettings,
        getFocusSessions,
        getActiveFocusSession,
        startFocusSession,
        pauseFocusSession,
        resumeFocusSession,
        extendFocusSession,
        concludeFocusSession,
        getDayBalance,
        getActivities,
        saveActivities,
        saveActivity,
        deleteActivity,
        duplicateActivity,
        getAllScheduleEntries,
        getScheduleEntries,
        saveScheduleEntry,
        updateScheduleEntry,
        deleteScheduleEntry,
        updateScheduleStatus,
        postponeScheduleEntry,
        getExecutionHistory,
        recordExecution,
        getRecommendationFeedbacks,
        recordRecommendationFeedback,
        clearLearningData,
        seedDemonstrationHistory,
        getContexts,
        getContextForActivity,
        saveContext,
        advanceContext,
        resetToDefault,
        defaultProfile
    };
})();
