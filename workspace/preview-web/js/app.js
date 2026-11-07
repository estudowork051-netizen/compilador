/**
 * LifeFlow Web Preview - Core Application Logic
 * Etapa 3/7: Planejamento inteligente, tempo disponível, agenda interativa, checklist e histórico.
 */
const App = (() => {

    // --- State Variables ---
    let selectedPlanningDate = new Date();
    let timePickerCallback = null;
    let pickerHour = 8;
    let pickerMinute = 0;

    let editingActivityId = null;
    let activityToDeleteId = null;

    // Filters for Activities tab
    let activityCategoryFilter = null;
    let activityTypeFilter = null;
    let activitySearchQuery = '';

    // Activity Sheet state
    let sheetType = 'FLEXIBLE'; // 'FLEXIBLE' | 'FIXED' | 'HABIT'
    let sheetCategory = 'Casa';
    let sheetDurationMinutes = 30;
    let sheetCount = 1;
    let sheetStartHour = 8;
    let sheetStartMinute = 0;
    let sheetEndHour = 10;
    let sheetEndMinute = 0;
    let sheetRecurrence = 'DAILY';
    let sheetWeeklyTarget = 3;

    // Schedule Entry Sheet state (Etapa 3)
    let schedSelectedActivityId = null;
    let schedStartHour = 9;
    let schedStartMinute = 0;
    let schedDurationMinutes = 60;

    // Active Task for Modals
    let activePromptEntryId = null;

    // --- Predefined Library ---
    const PREDEFINED_LIBRARY = [
        // Casa
        { title: 'Lavar louça', category: 'Casa', duration: 20, type: 'FLEXIBLE' },
        { title: 'Limpar quarto', category: 'Casa', duration: 30, type: 'FLEXIBLE' },
        { title: 'Cozinhar', category: 'Casa', duration: 60, type: 'FLEXIBLE' },
        { title: 'Lavar roupa', category: 'Casa', duration: 40, type: 'FLEXIBLE' },

        // Estudos
        { title: 'Estudar', category: 'Estudos', duration: 90, type: 'FLEXIBLE' },
        { title: 'Ler livro', category: 'Estudos', duration: 30, type: 'FLEXIBLE' },
        { title: 'Fazer exercícios', category: 'Estudos', duration: 60, type: 'FLEXIBLE' },
        { title: 'Revisar conteúdo', category: 'Estudos', duration: 45, type: 'FLEXIBLE' },

        // Saúde
        { title: 'Treinar', category: 'Saúde', duration: 60, type: 'HABIT' },
        { title: 'Caminhar', category: 'Saúde', duration: 45, type: 'HABIT' },
        { title: 'Alongar', category: 'Saúde', duration: 15, type: 'FLEXIBLE' },
        { title: 'Meditar', category: 'Saúde', duration: 15, type: 'FLEXIBLE' },

        // Trabalho
        { title: 'Trabalho / Estágio', category: 'Trabalho', duration: 360, type: 'FIXED' },
        { title: 'Reunião de alinhamento', category: 'Trabalho', duration: 60, type: 'FIXED' },
        { title: 'Organizar tarefas', category: 'Trabalho', duration: 30, type: 'FLEXIBLE' },

        // Lazer
        { title: 'Jogar videogame', category: 'Lazer', duration: 60, type: 'FLEXIBLE' },
        { title: 'Assistir filme', category: 'Lazer', duration: 120, type: 'FLEXIBLE' },
        { title: 'Descansar', category: 'Lazer', duration: 30, type: 'FLEXIBLE' },

        // Pessoal
        { title: 'Fazer compras', category: 'Pessoal', duration: 60, type: 'FLEXIBLE' },
        { title: 'Resolver pendências', category: 'Pessoal', duration: 45, type: 'FLEXIBLE' }
    ];

    const CATEGORY_COLORS = {
        'Casa': '#55D6BE',
        'Estudos': '#8B9CFF',
        'Saúde': '#4EBA6F',
        'Trabalho': '#FFA726',
        'Lazer': '#AB8BFF',
        'Pessoal': '#FF7597',
        'Outro': '#AAB3C5'
    };

    const STATUS_INFO = {
        'PENDING': { label: 'Pendente', color: '#9CA3AF', bg: 'rgba(156, 163, 175, 0.15)' },
        'IN_PROGRESS': { label: 'Em andamento', color: '#3B82F6', bg: 'rgba(59, 130, 246, 0.15)' },
        'COMPLETED': { label: 'Concluída', color: '#10B981', bg: 'rgba(16, 185, 129, 0.15)' },
        'POSTPONED': { label: 'Adiada', color: '#F59E0B', bg: 'rgba(245, 158, 11, 0.15)' },
        'NOT_DONE': { label: 'Não realizada', color: '#EF4444', bg: 'rgba(239, 68, 68, 0.15)' },
        'CANCELLED': { label: 'Cancelada', color: '#6B7280', bg: 'rgba(107, 114, 128, 0.15)' }
    };

    // --- String & Diacritics Utilities ---
    function removeAccents(str) {
        if (!str) return '';
        return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }

    function fuzzyContains(target, query) {
        if (!query || query.trim() === '') return true;
        const cleanTarget = removeAccents(target).toLowerCase();
        const cleanQuery = removeAccents(query).toLowerCase();
        return cleanTarget.includes(cleanQuery);
    }

    // --- Date & Time Utilities ---
    const PT_DAYS = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado'];
    const PT_DAYS_SHORT = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
    const PT_MONTHS = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'];
    const PT_MONTHS_SHORT = ['jan.', 'fev.', 'mar.', 'abr.', 'mai.', 'jun.', 'jul.', 'ago.', 'set.', 'out.', 'nov.', 'dez.'];

    function getGreeting(hour = new Date().getHours()) {
        if (hour >= 5 && hour < 12) return 'Bom dia';
        if (hour >= 12 && hour < 18) return 'Boa tarde';
        return 'Boa noite';
    }

    function formatDayOfWeek(date = new Date()) {
        const day = PT_DAYS[date.getDay()];
        return day.charAt(0).toUpperCase() + day.slice(1);
    }

    function toISODate(date) {
        if (!date) return '';
        if (typeof date === 'string') return date;
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    }

    function formatDisplayDate(date, refDate = new Date()) {
        const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        const r = new Date(refDate.getFullYear(), refDate.getMonth(), refDate.getDate());
        const diffDays = Math.round((d - r) / (1000 * 60 * 60 * 24));

        const dayNum = date.getDate();
        const monthName = PT_MONTHS[date.getMonth()];

        if (diffDays === 0) return `Hoje, ${dayNum} de ${monthName}`;
        if (diffDays === 1) return `Amanhã, ${dayNum} de ${monthName}`;
        if (diffDays === -1) return `Ontem, ${dayNum} de ${monthName}`;

        const dayName = PT_DAYS[date.getDay()];
        const shortMonth = PT_MONTHS_SHORT[date.getMonth()];
        const str = `${dayName}, ${dayNum} de ${shortMonth}`;
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    function parseTime(timeStr, fallbackHour = 8, fallbackMin = 0) {
        if (!timeStr || typeof timeStr !== 'string') return { hour: fallbackHour, minute: fallbackMin };
        const parts = timeStr.trim().split(':');
        if (parts.length !== 2) return { hour: fallbackHour, minute: fallbackMin };
        const hour = parseInt(parts[0], 10);
        const minute = parseInt(parts[1], 10);
        return {
            hour: isNaN(hour) ? fallbackHour : Math.max(0, Math.min(23, hour)),
            minute: isNaN(minute) ? fallbackMin : Math.max(0, Math.min(59, minute))
        };
    }

    function formatTime(hour, minute) {
        const h = String(hour).padStart(2, '0');
        const m = String(minute).padStart(2, '0');
        return `${h}:${m}`;
    }

    function formatMinutesToHours(minutes) {
        const h = Math.floor(minutes / 60);
        const m = minutes % 60;
        if (h > 0 && m > 0) return `${h}h ${m}min`;
        if (h > 0) return `${h}h`;
        return `${m}min`;
    }

    // =========================================================
    // TIME AVAILABILITY ENGINE (ETAPA 3)
    // =========================================================
    function timeStringToMinutes(timeStr) {
        const t = parseTime(timeStr);
        return t.hour * 60 + t.minute;
    }

    function getAwakeWindow(wakeUpStr, sleepStr) {
        const wakeMin = timeStringToMinutes(wakeUpStr);
        const rawSleepMin = timeStringToMinutes(sleepStr);
        const sleepMin = rawSleepMin <= wakeMin ? rawSleepMin + 1440 : rawSleepMin;
        return { start: wakeMin, end: sleepMin, duration: sleepMin - wakeMin };
    }

    function mergeIntervals(slots) {
        if (!slots || slots.length === 0) return [];
        const sorted = slots.filter(s => s.end > s.start).sort((a, b) => a.start - b.start);
        if (sorted.length === 0) return [];

        const merged = [];
        let current = { ...sorted[0] };

        for (let i = 1; i < sorted.length; i++) {
            const next = sorted[i];
            if (next.start <= current.end) {
                current.end = Math.max(current.end, next.end);
                current.label = `${current.label} + ${next.label}`;
            } else {
                merged.push(current);
                current = { ...next };
            }
        }
        merged.push(current);
        return merged;
    }

    function totalDurationOf(mergedSlots) {
        return mergedSlots.reduce((sum, s) => sum + Math.max(0, s.end - s.start), 0);
    }

    function clampToAwakeWindow(slot, awake) {
        let s = slot.start;
        let e = slot.end;
        if (e <= awake.start && awake.end > 1440) {
            s += 1440;
            e += 1440;
        }
        const clampedStart = Math.max(s, awake.start);
        const clampedEnd = Math.min(e, awake.end);
        if (clampedEnd > clampedStart) {
            return { start: clampedStart, end: clampedEnd, label: slot.label };
        }
        return null;
    }

    function findFreeSlots(awakeWindow, occupiedMergedSlots, minSlotMinutes = 15) {
        const freeSlots = [];
        let cursor = awakeWindow.start;

        for (const slot of occupiedMergedSlots) {
            if (slot.start > cursor) {
                const gap = slot.start - cursor;
                if (gap >= minSlotMinutes) {
                    freeSlots.push({ start: cursor, end: slot.start, duration: gap });
                }
            }
            cursor = Math.max(cursor, slot.end);
        }

        if (awakeWindow.end > cursor) {
            const gap = awakeWindow.end - cursor;
            if (gap >= minSlotMinutes) {
                freeSlots.push({ start: cursor, end: awakeWindow.end, duration: gap });
            }
        }
        return freeSlots;
    }

    function detectConflicts(slots) {
        const conflicts = [];
        for (let i = 0; i < slots.length; i++) {
            for (let j = i + 1; j < slots.length; j++) {
                const a = slots[i];
                const b = slots[j];
                if (a.start < b.end && b.start < a.end) {
                    const oStart = Math.max(a.start, b.start);
                    const oEnd = Math.min(a.end, b.end);
                    if (oEnd > oStart) {
                        conflicts.push({
                            titleA: a.label,
                            titleB: b.label,
                            start: oStart,
                            end: oEnd
                        });
                    }
                }
            }
        }
        return conflicts;
    }

    function isFixedActiveOnDate(activity, date) {
        if (activity.type !== 'FIXED') return true;
        const d = (typeof date === 'string') ? new Date(date + 'T12:00:00') : date;
        const dow = d.getDay(); // 0 is Sunday, 1 is Monday ... 6 is Saturday
        const dow1to7 = dow === 0 ? 7 : dow; // 1 = Monday ... 7 = Sunday
        const rec = activity.recurrenceType || 'DAILY';

        if (rec === 'DAILY') return true;
        if (rec === 'WEEKDAYS') return dow1to7 >= 1 && dow1to7 <= 5;
        if (rec === 'SPECIFIC_DAYS') {
            const days = activity.recurrenceDays || [];
            return days.length === 0 || days.includes(dow1to7);
        }
        return true;
    }

    function calculateDayAvailability(date, wakeUpStr, sleepStr, fixedActs, scheduledEntries, currentTimeDate = new Date()) {
        const awakeWindow = getAwakeWindow(wakeUpStr, sleepStr);
        const awakeDuration = awakeWindow.duration;

        // Fixed slots active for this date
        const activeFixed = fixedActs.filter(a => isFixedActiveOnDate(a, date));
        const rawFixedSlots = activeFixed.map(a => {
            const sMin = (a.startHour || 8) * 60 + (a.startMinute || 0);
            let eMin = (a.endHour || 10) * 60 + (a.endMinute || 0);
            if (eMin <= sMin) eMin += 1440;
            return { start: sMin, end: eMin, label: a.title };
        });

        const fixedSlots = rawFixedSlots.map(s => clampToAwakeWindow(s, awakeWindow)).filter(Boolean);
        const mergedFixed = mergeIntervals(fixedSlots);
        const fixedMinutes = totalDurationOf(mergedFixed);

        // Available time
        const availableTimeMinutes = Math.max(0, awakeDuration - fixedMinutes);

        // Planned slots (scheduled entries for this date not cancelled)
        const activeScheduled = scheduledEntries.filter(e => e.status !== 'CANCELLED');
        const rawPlannedSlots = activeScheduled.map(e => {
            const sMin = e.startHour * 60 + e.startMinute;
            const fullEnd = sMin + e.targetDurationMinutes;

            // Flexible tasks completed early only occupy the actual executed window, liberating the rest
            if (e.status === 'COMPLETED' && e.activityType !== 'FIXED') {
                const effDuration = e.actualDurationMinutes;
                if (effDuration !== null && effDuration !== undefined) {
                    if (effDuration <= 0) return null;
                    const effEnd = Math.min(fullEnd, sMin + effDuration);
                    if (effEnd <= sMin) return null;
                    return { start: sMin, end: effEnd, label: e.activityTitle };
                }
                if (e.completedAt) {
                    const compDate = new Date(e.completedAt);
                    const compDateStr = compDate.toISOString().split('T')[0];
                    if (compDateStr === toISODate(date)) {
                        const compMin = compDate.getHours() * 60 + compDate.getMinutes();
                        if (compMin <= sMin) return null;
                        if (compMin < fullEnd) return { start: sMin, end: compMin, label: e.activityTitle };
                    }
                }
            }
            return { start: sMin, end: fullEnd, label: e.activityTitle };
        }).filter(Boolean);

        const plannedSlots = rawPlannedSlots.map(s => clampToAwakeWindow(s, awakeWindow)).filter(Boolean);
        const mergedPlanned = mergeIntervals(plannedSlots);
        const plannedMinutes = totalDurationOf(mergedPlanned);

        // Remaining free time
        const remainingFreeMinutes = Math.max(0, availableTimeMinutes - plannedMinutes);
        const isOverbooked = plannedMinutes > availableTimeMinutes;
        const overbookedMinutes = isOverbooked ? (plannedMinutes - availableTimeMinutes) : 0;

        // Free slots discovery
        const allOccupied = [...fixedSlots, ...plannedSlots];
        const mergedOccupied = mergeIntervals(allOccupied);
        const rawFreeSlots = findFreeSlots(awakeWindow, mergedOccupied, 15);

        // Apply past-time constraints based on target date vs reference date
        const now = currentTimeDate;
        const todayStr = now.toISOString().split('T')[0];
        const targetDateStr = toISODate(date);
        const isToday = targetDateStr === todayStr;
        const isPastDate = targetDateStr < todayStr;
        const nowMinutes = now.getHours() * 60 + now.getMinutes();
        const minAllowedStart = Math.ceil(nowMinutes / 5) * 5;

        let freeSlots;
        if (isPastDate) {
            freeSlots = []; // No retroactive slots for past days
        } else if (isToday) {
            freeSlots = rawFreeSlots.map(slot => {
                const s = Math.max(slot.start, minAllowedStart);
                if (slot.end - s >= 15) {
                    return { start: s, end: slot.end, label: slot.label };
                }
                return null;
            }).filter(Boolean);
        } else {
            freeSlots = rawFreeSlots; // Future dates: full awake window available
        }

        // Conflicts
        const conflicts = detectConflicts(allOccupied);

        // Ratios for visual bar
        const fixedRatio = awakeDuration > 0 ? (fixedMinutes / awakeDuration) : 0;
        const plannedRatio = awakeDuration > 0 ? (plannedMinutes / awakeDuration) : 0;
        const freeRatio = Math.max(0, 1 - fixedRatio - plannedRatio);

        return {
            dateStr: targetDateStr,
            wakeUpStr,
            sleepStr,
            awakeDuration,
            fixedMinutes,
            availableTimeMinutes,
            plannedMinutes,
            remainingFreeMinutes,
            isOverbooked,
            overbookedMinutes,
            freeSlots,
            conflicts,
            fixedRatio,
            plannedRatio,
            freeRatio
        };
    }

    // =========================================================
    // ETAPA 4: LIFEFLOW CONTEXT, RECOMENDAÇÃO & AUTO-ORGANIZAÇÃO
    // =========================================================
    const ContextExtractor = {
        extract(title, desc = '', category = '') {
            const text = `${title} ${desc}`.trim();
            if (!text) return null;

            // 1. Series
            const sMatch = text.match(/(.*?)(?:s(\d+)\s*e(\d+)|ep[íi]s[oó]d?i?o?\.?\s*(\d+)|s[eé]rie\s+(.*?)\s*ep\.?\s*(\d+))/i);
            if (sMatch) {
                let name = (sMatch[1] || sMatch[5] || title).replace(/(assistir|ver)\s+/i, '').trim();
                const season = sMatch[2];
                const ep = sMatch[3] || sMatch[4] || sMatch[6] || 1;
                let label = `Episódio ${ep}`;
                if (season) label = `Temporada ${season} Episódio ${ep}`;
                return {
                    contentType: 'SERIES',
                    contentName: name || title,
                    progressLabel: label,
                    currentUnit: parseInt(ep, 10),
                    unitType: 'EPISODE'
                };
            }

            // 2. Book
            const bMatch = text.match(/(.*?)(?:cap[íi]t?u?l?o?\.?\s*(\d+)|livro\s+(.*?)\s*cap\.?\s*(\d+))/i);
            if (bMatch) {
                let name = (bMatch[1] || bMatch[3] || title).replace(/(ler|leitura)\s+/i, '').trim();
                const cap = bMatch[2] || bMatch[4] || 1;
                return {
                    contentType: 'BOOK',
                    contentName: name || title,
                    progressLabel: `Capítulo ${cap}`,
                    currentUnit: parseInt(cap, 10),
                    unitType: 'CHAPTER'
                };
            }

            // 3. Studies
            const stMatch = text.match(/(.*?)(?:m[oó]dulo\.?\s*(\d+)|aula\s*(\d+))/i);
            if (stMatch) {
                let name = (stMatch[1] || title).replace(/(estudar|curso)\s+/i, '').trim();
                const unit = stMatch[2] || stMatch[3] || 1;
                const unitName = stMatch[2] ? 'Módulo' : 'Aula';
                return {
                    contentType: 'STUDY_COURSE',
                    contentName: name || title,
                    progressLabel: `${unitName} ${unit}`,
                    currentUnit: parseInt(unit, 10),
                    unitType: 'MODULE'
                };
            }

            // 4. Workout
            if (/treino|academia|exerc[íi]cio/i.test(text)) {
                const focus = text.replace(/treino\s*(de)?\s*/i, '').trim();
                return {
                    contentType: 'WORKOUT',
                    contentName: focus || 'Geral',
                    progressLabel: 'Treino ativo',
                    currentUnit: 1,
                    unitType: 'SESSION'
                };
            }

            return null;
        }
    };

    const AutoOrganizationEngine = {
        generateProposal(targetDate, wakeUpStr, sleepStr, fixedActs, existingEntries, unallocatedActs, contextMap, balancePreference) {
            const avail = calculateDayAvailability(targetDate, wakeUpStr, sleepStr, fixedActs, existingEntries);
            const freeSlots = [...avail.freeSlots];
            const proposedItems = [];

            // Situação D — Dia completamente ocupado
            if (avail.remainingFreeMinutes < 15 || freeSlots.length === 0) {
                return {
                    date: toISODate(targetDate),
                    items: [],
                    totalPlannedMinutes: 0,
                    explanationMessage: 'Seu dia já está completamente preenchido com compromissos e tarefas. Quer revisar a agenda ou planejar amanhã?'
                };
            }

            // Situação B — Não existem atividades flexíveis cadastradas (Fallback inteligente)
            if (unallocatedActs.length === 0) {
                let suggestions = [];
                if (balancePreference === 'STUDY_FOCUS') {
                    suggestions = [
                        { title: 'Leitura / Estudo focado', category: 'Estudos', duration: 45, reason: 'Sugestão para seu perfil de estudos' },
                        { title: 'Revisão de Conteúdo', category: 'Estudos', duration: 30, reason: 'Revisão tranquila de matéria' }
                    ];
                } else if (balancePreference === 'REST_FOCUS') {
                    suggestions = [
                        { title: 'Momento de Descanso', category: 'Lazer', duration: 30, reason: 'Pausa para recarregar energias' },
                        { title: 'Alongamento e Respiração', category: 'Saúde', duration: 15, reason: 'Cuidado pessoal e relaxamento' }
                    ];
                } else {
                    suggestions = [
                        { title: 'Caminhada ao ar livre', category: 'Saúde', duration: 30, reason: 'Equilíbrio e bem-estar' },
                        { title: 'Leitura tranquila', category: 'Estudos', duration: 30, reason: 'Momento de foco suave' },
                        { title: 'Organizar ambiente', category: 'Casa', duration: 20, reason: 'Praticidade da rotina' }
                    ];
                }

                for (const slot of freeSlots) {
                    let slotStart = slot.startMinute;
                    const slotEnd = slot.endMinute;
                    for (const sugg of suggestions) {
                        if (proposedItems.some(p => p.title === sugg.title)) continue;
                        if (slotStart + sugg.duration <= slotEnd) {
                            const itemStartHour = Math.floor(slotStart / 60);
                            const itemStartMinute = slotStart % 60;
                            const endMin = slotStart + sugg.duration;
                            const itemEndHour = Math.floor(endMin / 60);
                            const itemEndMinute = endMin % 60;

                            proposedItems.push({
                                activityId: 0,
                                title: sugg.title,
                                category: sugg.category,
                                startTime: formatTime(itemStartHour, itemStartMinute),
                                endTime: formatTime(itemEndHour, itemEndMinute),
                                durationMinutes: sugg.duration,
                                reason: sugg.reason,
                                score: 100
                            });
                            slotStart += sugg.duration + 10;
                        }
                    }
                }

                const totalPlanned = proposedItems.reduce((acc, cur) => acc + cur.durationMinutes, 0);
                return {
                    date: toISODate(targetDate),
                    items: proposedItems,
                    totalPlannedMinutes: totalPlanned,
                    explanationMessage: 'Você não possui atividades cadastradas pendentes. Preparamos sugestões equilibradas para seu tempo livre com base no seu perfil.'
                };
            }

            // Situação A & E — Existem atividades flexíveis cadastradas
            // Score and prioritize candidates incorporating balancePreference
            const candidates = unallocatedActs.map(act => {
                let score = 50;
                if (act.priority === 'HIGH') score += 30;
                if (act.priority === 'LOW') score -= 20;

                const ctx = contextMap[act.id] || Storage.getContextForActivity(act.id);
                if (ctx && ctx.continuityStatus === 'IN_PROGRESS') score += 40;

                if (balancePreference === 'STUDY_FOCUS' && (act.category === 'Estudos' || act.category === 'Trabalho')) score += 35;
                if (balancePreference === 'REST_FOCUS' && (act.category === 'Lazer' || act.category === 'Saúde')) score += 30;
                if (balancePreference === 'BALANCED' && act.category === 'Lazer') score += 15;

                return { activity: act, score, context: ctx };
            }).sort((a, b) => b.score - a.score);

            // Fit into free slots with 10min buffer
            for (const slot of freeSlots) {
                let slotStart = slot.startMinute;
                const slotEnd = slot.endMinute;

                for (const cand of candidates) {
                    if (proposedItems.some(p => p.activityId === cand.activity.id)) continue;
                    const dur = cand.activity.estimatedDurationMinutes || 30;

                    if (slotStart + dur <= slotEnd) {
                        const itemStartHour = Math.floor(slotStart / 60);
                        const itemStartMinute = slotStart % 60;
                        const endMin = slotStart + dur;
                        const itemEndHour = Math.floor(endMin / 60);
                        const itemEndMinute = endMin % 60;

                        let reason = cand.activity.priority === 'HIGH' ? 'Prioridade alta' : 'Equilíbrio da rotina';
                        if (cand.context) reason = `Continuidade: ${cand.context.contentName || cand.context.progressLabel}`;
                        if (balancePreference === 'STUDY_FOCUS' && cand.activity.category === 'Estudos') reason = 'Foco no perfil de estudos';
                        if (balancePreference === 'REST_FOCUS' && cand.activity.category === 'Lazer') reason = 'Pausa recomendada';

                        proposedItems.push({
                            activityId: cand.activity.id,
                            title: cand.activity.title,
                            category: cand.activity.category,
                            startTime: formatTime(itemStartHour, itemStartMinute),
                            endTime: formatTime(itemEndHour, itemEndMinute),
                            durationMinutes: dur,
                            reason: reason,
                            score: cand.score
                        });

                        slotStart += dur + 10; // 10 min break
                    }
                }
            }

            const totalPlanned = proposedItems.reduce((acc, cur) => acc + cur.durationMinutes, 0);

            // Situação C — Existem atividades, mas nenhuma coube nos intervalos
            if (proposedItems.length === 0) {
                const minNeeded = Math.min(...unallocatedActs.map(a => a.estimatedDurationMinutes || 30));
                return {
                    date: toISODate(targetDate),
                    items: [],
                    totalPlannedMinutes: 0,
                    explanationMessage: `Suas atividades cadastradas precisam de mais tempo (${minNeeded}m) do que os intervalos disponíveis livres. Que tal ajustar as durações ou escolher outro dia?`
                };
            }

            const remainingFree = avail.remainingFreeMinutes - totalPlanned;
            const explanation = remainingFree >= 180
                ? `Organizamos ${proposedItems.length} atividade(s) respeitando seu perfil e preservamos ${formatMinutesToHours(remainingFree)} livres para seu descanso e bem-estar.`
                : `Organizamos ${proposedItems.length} atividade(s) nos seus melhores horários livres, com pausas de respiro.`;

            return {
                date: toISODate(targetDate),
                items: proposedItems,
                totalPlannedMinutes: totalPlanned,
                explanationMessage: explanation
            };
        },

        reorganizeRemainingDay(currentTime, targetDate, wakeUpStr, sleepStr, fixedActs, existingEntries, unallocatedActs, contextMap, balancePreference) {
            const currentMin = currentTime.getHours() * 60 + currentTime.getMinutes();
            const baseProposal = this.generateProposal(targetDate, wakeUpStr, sleepStr, fixedActs, existingEntries, unallocatedActs, contextMap, balancePreference);
            
            const validItems = baseProposal.items.filter(item => {
                const parsed = parseTime(item.startTime);
                const m = parsed.hour * 60 + parsed.minute;
                return m >= currentMin;
            });

            baseProposal.items = validItems;
            baseProposal.totalPlannedMinutes = validItems.reduce((acc, c) => acc + c.durationMinutes, 0);
            baseProposal.explanationMessage = `Reorganizamos o restante do seu dia a partir de ${formatTime(currentTime.getHours(), currentTime.getMinutes())}.`;
            return baseProposal;
        }
    };

    const RecommendationEngine = {
        generateRecommendation(avail, activities, scheduledToday, profile) {
            const contexts = Storage.getContexts();
            const activeContext = contexts.find(c => c.continuityStatus === 'IN_PROGRESS');

            const now = new Date();
            const nowMin = now.getHours() * 60 + now.getMinutes();

            // 1. Continuidade
            if (activeContext) {
                const act = activities.find(a => a.id === activeContext.activityId);
                if (act) {
                    const nextUnit = (activeContext.currentUnit || 1) + 1;
                    const unitLabel = activeContext.contentType === 'SERIES' ? `Episódio ${nextUnit}` : `Capítulo ${nextUnit}`;
                    return {
                        type: 'RESUME_CONTENT',
                        title: `Continuar ${activeContext.contentName || act.title}`,
                        subtitle: `Você tem tempo livre. Que tal o ${unitLabel}?`,
                        reason: `Continuidade de ${activeContext.contentType === 'SERIES' ? 'série' : 'leitura'}`,
                        suggestedActivity: act,
                        suggestedDurationMinutes: act.estimatedDurationMinutes || 45
                    };
                }
            }

            // 2. Encaixe em intervalo livre
            const currentFreeSlot = (avail && avail.freeSlots) ? avail.freeSlots.find(s => nowMin >= s.startMinute && nowMin <= s.endMinute) : null;
            if (currentFreeSlot) {
                const gapDuration = currentFreeSlot.endMinute - nowMin;
                const fitAct = activities.find(a => a.type === 'FLEXIBLE' && a.estimatedDurationMinutes <= gapDuration);
                if (fitAct) {
                    return {
                        type: 'FILL_GAP',
                        title: `Encaixar "${fitAct.title}"`,
                        subtitle: `Você tem ${gapDuration}min livres agora. Esta tarefa leva ${fitAct.estimatedDurationMinutes}min.`,
                        reason: `Encaixe ideal no seu horário livre`,
                        suggestedActivity: fitAct,
                        suggestedDurationMinutes: fitAct.estimatedDurationMinutes
                    };
                }
            }

            return null;
        }
    };

    const HabitLearningEngine = {
        STANDARD_SLOTS: [
            { name: 'Madrugada (00h-06h)', startHour: 0, endHour: 6 },
            { name: 'Manhã cedo (06h-09h)', startHour: 6, endHour: 9 },
            { name: 'Manhã (09h-12h)', startHour: 9, endHour: 12 },
            { name: 'Início da tarde (12h-15h)', startHour: 12, endHour: 15 },
            { name: 'Tarde (15h-18h)', startHour: 15, endHour: 18 },
            { name: 'Noite (18h-21h)', startHour: 18, endHour: 21 },
            { name: 'Fim da noite (21h-00h)', startHour: 21, endHour: 24 }
        ],

        analyzeActivityTimeSlots(activityId, title, records) {
            const rel = records.filter(r => r.activityId == activityId);
            if (rel.length === 0) return [];
            const results = [];

            this.STANDARD_SLOTS.forEach(slot => {
                const slotRecords = rel.filter(r => {
                    const mins = r.plannedStartMinutes || 0;
                    const h = Math.floor(mins / 60) % 24;
                    return h >= slot.startHour && h < slot.endHour;
                });
                const completed = slotRecords.filter(r => r.finalStatus === 'COMPLETED').length;
                const notDone = slotRecords.filter(r => r.finalStatus === 'NOT_DONE' || r.finalStatus === 'CANCELLED').length;
                const postponed = slotRecords.filter(r => r.finalStatus === 'POSTPONED').length;
                const total = completed + notDone + postponed;
                if (total === 0) return;

                const rate = completed / total;
                let confidence = 'INSUFFICIENT_DATA';
                if (total >= 5 && rate >= 0.6) confidence = 'MODERATE';
                else if (total >= 3) confidence = 'LOW';

                results.push({
                    activityId,
                    title,
                    slotName: slot.name,
                    startHour: slot.startHour,
                    endHour: slot.endHour,
                    completed,
                    total,
                    rate,
                    confidence,
                    reason: `Taxa de conclusão de ${Math.round(rate * 100)}% em ${total} registros analisados.`
                });
            });

            return results.sort((a, b) => b.rate - a.rate);
        },

        findBestTimeSlot(activityId, title, records) {
            const list = this.analyzeActivityTimeSlots(activityId, title, records);
            return list.find(s => s.total >= 3 && s.rate >= 0.6) || null;
        },

        estimateDuration(activityId, title, plannedDuration, records) {
            const valid = records
                .filter(r => r.activityId == activityId && r.finalStatus === 'COMPLETED' && r.actualDurationMinutes && r.actualDurationMinutes >= 5 && r.actualDurationMinutes <= 480)
                .map(r => r.actualDurationMinutes);

            if (valid.length < 3) return null;

            valid.sort((a, b) => a - b);
            const mid = Math.floor(valid.length / 2);
            const median = valid.length % 2 !== 0 ? valid[mid] : Math.round((valid[mid - 1] + valid[mid]) / 2);
            const diff = median - plannedDuration;

            return {
                activityId,
                title,
                plannedDuration,
                learnedMedian: median,
                sampleCount: valid.length,
                difference: diff,
                suggestion: `Você costuma terminar essa atividade em cerca de ${median} minutos (planejado: ${plannedDuration}m). Quer atualizar a duração sugerida?`
            };
        },

        detectPostponed(records) {
            const grouped = {};
            records.forEach(r => {
                if (!grouped[r.activityId]) grouped[r.activityId] = [];
                grouped[r.activityId].push(r);
            });

            const insights = [];
            Object.keys(grouped).forEach(actId => {
                const list = grouped[actId];
                const total = list.length;
                if (total < 3) return;
                const postponed = list.filter(r => r.finalStatus === 'POSTPONED').length;
                if (postponed >= 3 && (postponed / total) >= 0.35) {
                    const title = list[0].activityTitle || 'Atividade';
                    const rate = Math.round((postponed / total) * 100);
                    insights.push({
                        activityId: actId,
                        title,
                        postponedCount: postponed,
                        total,
                        rate,
                        suggestion: `Percebi que '${title}' foi adiada ${postponed} vezes recentemente (${rate}% dos agendamentos). Que tal tentar um horário ou duração diferente?`
                    });
                }
            });
            return insights;
        },

        computeSummary(records, activities, scheduledToday) {
            const completed = records.filter(r => r.finalStatus === 'COMPLETED');
            const hasSufficientData = completed.length >= 3;

            const bestTimes = [];
            const durationInsights = [];
            activities.forEach(a => {
                const best = this.findBestTimeSlot(a.id, a.title, records);
                if (best) bestTimes.push(best);

                const dur = this.estimateDuration(a.id, a.title, a.estimatedDurationMinutes || 60, records);
                if (dur && Math.abs(dur.difference) >= 10) durationInsights.push(dur);
            });

            const postponed = this.detectPostponed(records);

            const categoryMinutes = {};
            completed.forEach(r => {
                const cat = r.category || 'Geral';
                const dur = r.actualDurationMinutes || r.plannedDurationMinutes || 30;
                categoryMinutes[cat] = (categoryMinutes[cat] || 0) + dur;
            });

            return {
                completedCount: completed.length,
                totalRecords: records.length,
                hasSufficientData,
                bestTimes,
                durationInsights,
                postponed,
                categoryMinutes
            };
        },

        generateLearnedRecommendations(nowMin, profile, learningSettings, activities, records, contexts, feedbacks, todayEntries) {
            if (!learningSettings.learningEnabled) {
                return RecommendationEngine.generateRecommendation({ freeSlots: [] }, activities, todayEntries, profile);
            }

            const recs = [];
            const scheduledIds = new Set(todayEntries.filter(e => e.status !== 'CANCELLED').map(e => e.activityId));
            const notInterestedIds = new Set(feedbacks.filter(f => f.action === 'NOT_INTERESTED').map(f => f.activityId));

            // 1. Best time slot recommendation
            activities.forEach(a => {
                if (scheduledIds.has(a.id) || notInterestedIds.has(a.id)) return;
                const best = this.findBestTimeSlot(a.id, a.title, records);
                if (best) {
                    const currentHour = Math.floor(nowMin / 60) % 24;
                    if (currentHour >= (best.startHour - 1) && currentHour <= best.endHour) {
                        recs.push({
                            type: 'BEST_TIME_SLOT',
                            title: `Melhor horário para ${a.title}`,
                            subtitle: `Você costuma concluir ${a.title} com mais frequência por volta das ${String(best.startHour).padStart(2, '0')}:00. Quer reservar esse horário?`,
                            reason: best.reason,
                            suggestedActivity: a,
                            suggestedDurationMinutes: a.estimatedDurationMinutes || 60,
                            suggestedStartHour: best.startHour,
                            suggestedStartMinute: 0,
                            actionLabel: 'Reservar horário',
                            priorityScore: 150
                        });
                    }
                }
            });

            // 2. Duration insight recommendation
            activities.forEach(a => {
                if (notInterestedIds.has(a.id)) return;
                const dur = this.estimateDuration(a.id, a.title, a.estimatedDurationMinutes || 60, records);
                if (dur && Math.abs(dur.difference) >= 10) {
                    recs.push({
                        type: 'LEARNED_DURATION',
                        title: `Ajuste de duração: ${a.title}`,
                        subtitle: dur.suggestion,
                        reason: `Baseado em ${dur.sampleCount} execuções reais com duração mediana de ${dur.learnedMedian} min.`,
                        suggestedActivity: a,
                        suggestedDurationMinutes: dur.learnedMedian,
                        actionLabel: `Atualizar para ${dur.learnedMedian}m`,
                        priorityScore: 120
                    });
                }
            });

            // 3. Postponed adjustment
            const postponedList = this.detectPostponed(records);
            postponedList.slice(0, 1).forEach(p => {
                const a = activities.find(act => act.id == p.activityId);
                if (a && !notInterestedIds.has(a.id)) {
                    recs.push({
                        type: 'POSTPONED_ADJUSTMENT',
                        title: `Novo horário para ${a.title}`,
                        subtitle: p.suggestion,
                        reason: `Identificamos ${p.postponedCount} reagendamentos anteriores.`,
                        suggestedActivity: a,
                        suggestedDurationMinutes: a.estimatedDurationMinutes || 60,
                        actionLabel: 'Tentar novo horário',
                        priorityScore: 110
                    });
                }
            });

            // 4. Fallback to continuity or gap fit
            const step4Rec = RecommendationEngine.generateRecommendation({ freeSlots: [] }, activities, todayEntries, profile);
            if (step4Rec) {
                step4Rec.priorityScore = 90;
                step4Rec.actionLabel = 'Continuar agora';
                recs.push(step4Rec);
            }

            return recs.sort((a, b) => (b.priorityScore || 0) - (a.priorityScore || 0))[0] || null;
        }
    };

    // --- Theme Management ---
    function applyTheme() {
        const profile = Storage.getProfile();
        let isDark = false;
        if (profile.themeMode === 'DARK') isDark = true;
        else if (profile.themeMode === 'LIGHT') isDark = false;
        else isDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;

        const phoneFrame = document.getElementById('phone-frame');
        const themeAttr = isDark ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', themeAttr);
        if (phoneFrame) phoneFrame.setAttribute('data-theme', themeAttr);
    }

    function setTheme(mode) {
        Storage.updateField('themeMode', mode);
        applyTheme();
        refreshSettings();
    }

    // --- Onboarding Logic ---
    let currentOnboardingStep = 1;

    function initOnboarding() {
        const profile = Storage.getProfile();
        currentOnboardingStep = profile.onboardingStep || 1;
        if (currentOnboardingStep < 1 || currentOnboardingStep > 6) currentOnboardingStep = 1;

        const nameInput = document.getElementById('onboarding-input-name');
        if (nameInput) nameInput.value = profile.name || '';

        updateOnboardingTimesDisplay();
        updateOnboardingCommitmentsDisplay();
        updateOnboardingOrgModeDisplay();
        showOnboardingStep(currentOnboardingStep);

        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
            const p = Storage.getProfile();
            if (p.themeMode === 'SYSTEM') applyTheme();
        });
    }

    function showOnboardingStep(step) {
        currentOnboardingStep = step;
        Storage.updateField('onboardingStep', step);

        for (let i = 1; i <= 6; i++) {
            const stepEl = document.getElementById(`onboarding-step-${i}`);
            if (stepEl) {
                if (i === step) stepEl.classList.remove('hidden');
                else stepEl.classList.add('hidden');
            }
        }

        const backBtn = document.getElementById('onboarding-btn-back');
        if (backBtn) backBtn.style.visibility = step > 1 ? 'visible' : 'hidden';

        const progressDots = document.querySelectorAll('.step-progress-dot');
        progressDots.forEach((dot, idx) => {
            const dotStep = idx + 1;
            dot.classList.remove('active', 'completed');
            if (dotStep === step) dot.classList.add('active');
            else if (dotStep < step) dot.classList.add('completed');
        });

        if (step === 3) updateOnboardingTimesDisplay();
        else if (step === 6) updateOnboardingSummary();
    }

    function nextOnboardingStep() {
        if (currentOnboardingStep < 6) showOnboardingStep(currentOnboardingStep + 1);
    }

    function prevOnboardingStep() {
        if (currentOnboardingStep > 1) showOnboardingStep(currentOnboardingStep - 1);
    }

    function updateOnboardingTimesDisplay() {
        const profile = Storage.getProfile();
        const wakeBtn = document.getElementById('onboarding-wake-btn');
        const sleepBtn = document.getElementById('onboarding-sleep-btn');
        const durationText = document.getElementById('onboarding-awake-duration');

        if (wakeBtn) wakeBtn.innerText = profile.wakeUpTime;
        if (sleepBtn) sleepBtn.innerText = profile.sleepTime;

        if (durationText) {
            const min = getAwakeWindow(profile.wakeUpTime, profile.sleepTime).duration;
            durationText.innerText = `${formatMinutesToHours(min)} acordado`;
        }
    }

    function updateOnboardingCommitmentsDisplay() {
        const profile = Storage.getProfile();
        const chips = document.querySelectorAll('.onboarding-chip');
        chips.forEach(chip => {
            const val = chip.getAttribute('data-value');
            if (profile.fixedCommitments && profile.fixedCommitments.includes(val)) {
                chip.classList.add('selected');
            } else {
                chip.classList.remove('selected');
            }
        });
    }

    function toggleOnboardingCommitment(commitmentVal) {
        const profile = Storage.getProfile();
        let list = [...(profile.fixedCommitments || [])];

        if (commitmentVal === 'Configurar depois') {
            list = ['Configurar depois'];
        } else {
            list = list.filter(item => item !== 'Configurar depois');
            if (list.includes(commitmentVal)) {
                list = list.filter(item => item !== commitmentVal);
            } else {
                list.push(commitmentVal);
            }
        }

        Storage.updateField('fixedCommitments', list);
        updateOnboardingCommitmentsDisplay();
    }

    function updateOnboardingOrgModeDisplay() {
        const profile = Storage.getProfile();
        document.querySelectorAll('.org-mode-card').forEach(card => {
            const mode = card.getAttribute('data-mode');
            card.classList.toggle('selected', mode === profile.organizationMode);
        });
    }

    function setOrganizationMode(mode) {
        Storage.updateField('organizationMode', mode);
        updateOnboardingOrgModeDisplay();
        refreshSettings();
    }

    function updateOnboardingSummary() {
        const profile = Storage.getProfile();
        const summaryName = document.getElementById('summary-row-name');
        const summaryNameVal = document.getElementById('summary-val-name');
        const summaryTimes = document.getElementById('summary-val-times');
        const summaryMode = document.getElementById('summary-val-mode');
        const summaryCommitments = document.getElementById('summary-val-commitments');

        if (summaryName && summaryNameVal) {
            if (profile.name && profile.name.trim().length > 0) {
                summaryName.style.display = 'flex';
                summaryNameVal.innerText = profile.name.trim();
            } else {
                summaryName.style.display = 'none';
            }
        }

        if (summaryTimes) {
            const min = getAwakeWindow(profile.wakeUpTime, profile.sleepTime).duration;
            summaryTimes.innerText = `${profile.wakeUpTime} às ${profile.sleepTime} (${formatMinutesToHours(min)})`;
        }

        if (summaryMode) {
            summaryMode.innerText = profile.organizationMode === 'MANUAL' ? 'Manual' : 'Automático';
        }

        if (summaryCommitments) {
            summaryCommitments.innerText = (profile.fixedCommitments && profile.fixedCommitments.length > 0)
                ? profile.fixedCommitments.join(', ')
                : 'Nenhum';
        }
    }

    function finishOnboarding() {
        Storage.updateField('onboardingCompleted', true);
        Navigation.showMainScreen();
    }

    // =========================================================
    // DYNAMIC AVAILABILITY UI COMPONENT (CARD + BAR)
    // =========================================================
    function updateAvailabilityCardUI(prefix, avail) {
        const titleEl = document.getElementById(`${prefix}-avail-title`);
        const rangeEl = document.getElementById(`${prefix}-avail-range`);
        const barFixed = document.getElementById(`${prefix}-bar-fixed`);
        const barPlanned = document.getElementById(`${prefix}-bar-planned`);
        const barFree = document.getElementById(`${prefix}-bar-free`);
        const statAvail = document.getElementById(`${prefix}-stat-available`);
        const statPlanned = document.getElementById(`${prefix}-stat-planned`);
        const statFree = document.getElementById(`${prefix}-stat-free`);
        const warningBox = document.getElementById(`${prefix}-avail-warning`);
        const warningText = document.getElementById(`${prefix}-avail-warning-text`);

        if (titleEl) {
            if (avail.isOverbooked) titleEl.innerText = 'Atenção ao tempo planejado';
            else if (avail.remainingFreeMinutes === 0 && avail.plannedMinutes > 0) titleEl.innerText = 'Dia 100% preenchido!';
            else if (avail.plannedMinutes > 0) titleEl.innerText = 'Seu dia está quase organizado!';
            else titleEl.innerText = 'Dia livre para planejar';
        }

        if (rangeEl) rangeEl.innerText = `${avail.wakeUpStr} → ${avail.sleepStr}`;

        if (barFixed) barFixed.style.width = `${Math.round(avail.fixedRatio * 100)}%`;
        if (barPlanned) barPlanned.style.width = `${Math.round(avail.plannedRatio * 100)}%`;
        if (barFree) barFree.style.width = `${avail.isOverbooked ? 0 : Math.round(avail.freeRatio * 100)}%`;

        if (statAvail) statAvail.innerText = formatMinutesToHours(avail.availableTimeMinutes);
        if (statPlanned) statPlanned.innerText = formatMinutesToHours(avail.plannedMinutes);
        if (statFree) statFree.innerText = formatMinutesToHours(avail.remainingFreeMinutes);

        if (warningBox && warningText) {
            if (avail.isOverbooked) {
                warningBox.classList.remove('hidden');
                warningText.innerText = `Seu planejamento ultrapassou o tempo disponível em ${avail.overbookedMinutes} min. Quer ajustar algumas atividades?`;
            } else if (avail.conflicts.length > 0) {
                warningBox.classList.remove('hidden');
                warningText.innerText = 'Há atividades sobrepostas neste dia. Verifique os horários na agenda.';
            } else {
                warningBox.classList.add('hidden');
            }
        }
    }

    // =========================================================
    // HOME SCREEN LOGIC
    // =========================================================
    // =========================================================
    // HOME SCREEN LOGIC
    // =========================================================
    function refreshHome() {
        const profile = Storage.getProfile();
        const activities = Storage.getActivities();
        const today = new Date();
        const todayStr = toISODate(today);
        const scheduledToday = Storage.getScheduleEntries(todayStr);

        // Header
        const greetingEl = document.getElementById('home-header-greeting');
        const dateSubEl = document.getElementById('home-header-date');
        const hour = today.getHours();
        const icon = hour >= 5 && hour < 12 ? '☀️' : (hour >= 12 && hour < 18 ? '🌤️' : '🌙');
        const greeting = getGreeting(hour);
        const namePart = profile.name && profile.name.trim().length > 0 ? `, ${profile.name.trim()}` : '';

        if (greetingEl) greetingEl.innerText = `${greeting}${namePart}! ${icon}`;
        if (dateSubEl) dateSubEl.innerText = 'Seu dia está tomando forma.';

        // Dynamic Availability Calculation for Today
        const avail = calculateDayAvailability(today, profile.wakeUpTime, profile.sleepTime, activities.filter(a => a.type === 'FIXED'), scheduledToday);

        // Update Nature Routine Card (Seção 1 & 2)
        const routineCard = document.getElementById('home-nature-routine-card');
        if (routineCard) {
            routineCard.className = 'nature-routine-card ' + (hour >= 5 && hour < 12 ? 'nature-gradient-morning' : (hour >= 12 && hour < 18 ? 'nature-gradient-afternoon' : 'nature-gradient-night'));
        }
        const atmoIcon = document.getElementById('home-nature-period-icon');
        const atmoText = document.getElementById('home-nature-atmosphere');
        if (atmoIcon && atmoText) {
            if (hour >= 5 && hour < 12) {
                atmoIcon.innerText = '☀️';
                atmoText.innerText = 'Manhã fresca';
            } else if (hour >= 12 && hour < 18) {
                atmoIcon.innerText = '🌤️';
                atmoText.innerText = 'Tarde produtiva';
            } else {
                atmoIcon.innerText = '🌙';
                atmoText.innerText = 'Noite serena';
            }
        }
        const clockEl = document.getElementById('home-nature-clock');
        if (clockEl) {
            clockEl.innerText = `${String(today.getHours()).padStart(2, '0')}:${String(today.getMinutes()).padStart(2, '0')}`;
        }

        const nowMin = today.getHours() * 60 + today.getMinutes();
        const inProgress = scheduledToday.find(e => e.status === 'IN_PROGRESS' || (e.status === 'PENDING' && nowMin >= (e.startHour * 60 + e.startMinute) && nowMin < (e.startHour * 60 + e.startMinute + e.targetDurationMinutes)));
        const upcoming = !inProgress ? scheduledToday.filter(e => e.status === 'PENDING' && (e.startHour * 60 + e.startMinute) > nowMin).sort((a, b) => (a.startHour * 60 + a.startMinute) - (b.startHour * 60 + b.startMinute))[0] : null;
        const allDone = scheduledToday.length > 0 && scheduledToday.every(e => e.status === 'COMPLETED');

        const statusKicker = document.getElementById('home-nature-status-kicker');
        const statusTitle = document.getElementById('home-nature-status-title');
        const statusSub = document.getElementById('home-nature-status-sub');

        if (inProgress) {
            if (statusKicker) statusKicker.innerText = 'EM ANDAMENTO AGORA';
            if (statusTitle) statusTitle.innerText = inProgress.activityTitle || 'Atividade atual';
            const endM = inProgress.startHour * 60 + inProgress.startMinute + inProgress.targetDurationMinutes;
            if (statusSub) statusSub.innerText = `Termina às ${formatTime(Math.floor(endM / 60) % 24, endM % 60)} (${Math.max(1, endM - nowMin)} min restantes)`;
        } else if (upcoming) {
            const minsUntil = (upcoming.startHour * 60 + upcoming.startMinute) - nowMin;
            if (statusKicker) statusKicker.innerText = 'PRÓXIMA ATIVIDADE';
            if (statusTitle) statusTitle.innerText = upcoming.activityTitle || 'Próxima tarefa';
            if (statusSub) statusSub.innerText = minsUntil <= 60 ? `Começa em ${minsUntil} minutos • Duração: ${formatMinutesToHours(upcoming.targetDurationMinutes)}` : `Começa às ${formatTime(upcoming.startHour, upcoming.startMinute)} • Duração: ${formatMinutesToHours(upcoming.targetDurationMinutes)}`;
        } else if (allDone) {
            if (statusKicker) statusKicker.innerText = 'TUDO CONCLUÍDO';
            if (statusTitle) statusTitle.innerText = 'Dia concluído com tranquilidade 🌿';
            if (statusSub) statusSub.innerText = 'Aproveite seu tempo livre com descanso e leveza.';
        } else {
            if (statusKicker) statusKicker.innerText = 'DIA LIVRE';
            if (statusTitle) statusTitle.innerText = 'Seu dia está livre agora';
            if (statusSub) statusSub.innerText = 'Organize seus blocos de foco e lazer quando quiser.';
        }

        const freeVal = document.getElementById('home-nature-free-text');
        const plannedVal = document.getElementById('home-nature-planned-text');
        if (freeVal) freeVal.innerText = formatMinutesToHours(avail.remainingFreeMinutes);
        if (plannedVal) plannedVal.innerText = formatMinutesToHours(avail.plannedActivitiesMinutes);

        // Render Scheduled for Today on Home with reorder support (Seção 3 & 4)
        const countBadge = document.getElementById('home-today-count-badge');
        if (countBadge) countBadge.innerText = scheduledToday.length;

        const schedContainer = document.getElementById('home-scheduled-container');
        const schedEmpty = document.getElementById('home-scheduled-empty');

        if (schedContainer && schedEmpty) {
            if (scheduledToday.length > 0) {
                schedEmpty.classList.add('hidden');
                schedContainer.innerHTML = scheduledToday.map((e, idx) => renderScheduledCardHtml(e, idx, scheduledToday.length)).join('');
            } else {
                schedEmpty.classList.remove('hidden');
                schedContainer.innerHTML = '';
            }
        }

        // Smart Recommendation Card (Etapa 5 / Seção 4)
        renderSmartRecommendation(avail, activities, scheduledToday, profile);

        // Prominent Main Button: "Organizar meu dia"
        const btnOrganize = document.getElementById('home-btn-organize-day');
        if (btnOrganize && !btnOrganize.dataset.bound) {
            btnOrganize.dataset.bound = 'true';
            btnOrganize.addEventListener('click', () => {
                Navigation.goToTab('planning');
            });
        }

        // Render Fixed Commitments on Home (discreet secondary)
        const fixedToday = activities.filter(a => a.type === 'FIXED' && isFixedActiveOnDate(a, today));
        const fixedSection = document.getElementById('home-fixed-section');
        const fixedContainer = document.getElementById('home-fixed-container');

        if (fixedSection && fixedContainer) {
            if (fixedToday.length > 0) {
                fixedSection.classList.remove('hidden');
                fixedContainer.innerHTML = fixedToday.map(a => renderActivityCardHtml(a)).join('');
            } else {
                fixedSection.classList.add('hidden');
                fixedContainer.innerHTML = '';
            }
        }

        updateFocusSessionUI();
        updateDayTrackingUI(scheduledToday, avail);
        setupTimelineEventListeners();

        // Environment selector chips
        document.querySelectorAll('.env-chip').forEach(chip => {
            if (!chip.dataset.bound) {
                chip.dataset.bound = 'true';
                chip.addEventListener('click', () => {
                    document.querySelectorAll('.env-chip').forEach(c => c.classList.remove('active'));
                    chip.classList.add('active');
                    const env = chip.dataset.env;
                    const routineCard = document.getElementById('home-nature-routine-card');
                    if (routineCard) {
                        routineCard.classList.remove('nature-gradient-morning', 'nature-gradient-afternoon', 'nature-gradient-night');
                        routineCard.classList.add(`nature-gradient-${env}`);
                    }
                });
            }
        });

        // Mood bar chips
        const moodFeedbacks = {
            calm: { icon: '🍃', text: 'Que serenidade boa. Mantenha essa paz no seu ritmo.' },
            focused: { icon: '⚡', text: 'Excelente momento de energia. Aproveite para avançar no que importa.' },
            tired: { icon: '☕', text: 'Seu corpo pede calma. Respeite seus limites e faça pausas curtas.' },
            inspired: { icon: '✨', text: 'Sinta essa faísca criativa! Um ótimo instante para criar.' }
        };
        document.querySelectorAll('.mood-chip').forEach(chip => {
            if (!chip.dataset.bound) {
                chip.dataset.bound = 'true';
                chip.addEventListener('click', () => {
                    document.querySelectorAll('.mood-chip').forEach(c => c.classList.remove('selected'));
                    chip.classList.add('selected');
                    const mood = chip.dataset.mood;
                    const feedback = moodFeedbacks[mood];
                    const feedbackEl = document.getElementById('mood-feedback');
                    const iconEl = document.getElementById('mood-feedback-icon');
                    const textEl = document.getElementById('mood-feedback-text');
                    if (feedbackEl && feedback) {
                        if (iconEl) iconEl.innerText = feedback.icon;
                        if (textEl) textEl.innerText = feedback.text;
                        feedbackEl.classList.remove('hidden');
                    }
                });
            }
        });

        // Hydration tracker
        let currentWaterMl = 750;
        const btnAddWater = document.getElementById('btn-add-water');
        if (btnAddWater && !btnAddWater.dataset.bound) {
            btnAddWater.dataset.bound = 'true';
            btnAddWater.addEventListener('click', () => {
                currentWaterMl = Math.min(2000, currentWaterMl + 250);
                const statusEl = document.getElementById('hydration-status-text');
                const fillEl = document.getElementById('hydration-wave-fill');
                if (statusEl) statusEl.innerText = `${currentWaterMl.toLocaleString('pt-BR')} / 2.000 ml`;
                if (fillEl) fillEl.style.width = `${(currentWaterMl / 2000) * 100}%`;
            });
        }

        // Breathe modal
        const btnBreathe = document.getElementById('home-btn-breathe');
        const modalBreathe = document.getElementById('modal-breathe');
        const btnBreatheClose = document.getElementById('breathe-btn-close');
        const btnBreatheToggle = document.getElementById('breathe-btn-toggle');
        if (btnBreathe && modalBreathe && !btnBreathe.dataset.bound) {
            btnBreathe.dataset.bound = 'true';
            btnBreathe.addEventListener('click', () => {
                modalBreathe.classList.remove('hidden');
            });
        }
        if (btnBreatheClose && modalBreathe && !btnBreatheClose.dataset.bound) {
            btnBreatheClose.dataset.bound = 'true';
            btnBreatheClose.addEventListener('click', () => {
                modalBreathe.classList.add('hidden');
            });
        }
        if (btnBreatheToggle && !btnBreatheToggle.dataset.bound) {
            btnBreatheToggle.dataset.bound = 'true';
            let breathing = true;
            btnBreatheToggle.addEventListener('click', () => {
                breathing = !breathing;
                btnBreatheToggle.innerText = breathing ? 'Pausar' : 'Retomar';
                const ring = document.querySelector('.breathe-ring');
                if (ring) {
                    ring.style.animationPlayState = breathing ? 'running' : 'paused';
                }
            });
        }
    }

    let currentRecommendation = null;
    let isRecommendationDismissed = false;

    function renderSmartRecommendation(avail, activities, scheduledToday, profile) {
        const card = document.getElementById('home-recommendation-card');
        if (!card) return;

        if (isRecommendationDismissed) {
            card.classList.add('hidden');
            return;
        }

        const learningSettings = Storage.getLearningSettings();
        const records = Storage.getExecutionHistory();
        const contexts = Storage.getContexts();
        const feedbacks = Storage.getRecommendationFeedbacks();
        const nowMin = new Date().getHours() * 60 + new Date().getMinutes();

        const rec = HabitLearningEngine.generateLearnedRecommendations(
            nowMin,
            profile,
            learningSettings,
            activities,
            records,
            contexts,
            feedbacks,
            scheduledToday
        );
        currentRecommendation = rec;

        if (!rec) {
            card.classList.add('hidden');
            return;
        }

        card.classList.remove('hidden');
        const badgeEl = document.getElementById('rec-badge');
        const titleEl = document.getElementById('rec-title');
        const subEl = document.getElementById('rec-subtitle');
        const reasonEl = document.getElementById('rec-reason-box');
        const acceptBtn = document.getElementById('rec-btn-accept');

        if (badgeEl) {
            const badgeText = rec.type === 'BEST_TIME_SLOT' ? '⭐ Melhor Horário Observado'
                : rec.type === 'LEARNED_DURATION' ? '⏱ Duração Habitual'
                : rec.type === 'POSTPONED_ADJUSTMENT' ? '🔄 Ajuste Amigável'
                : rec.type === 'RESUME_CONTENT' ? '📖 Continuidade'
                : '✨ Sugestão LifeFlow';
            badgeEl.innerText = badgeText;
        }

        if (titleEl) titleEl.innerText = rec.title;
        if (subEl) subEl.innerText = rec.subtitle;
        if (reasonEl) {
            if (learningSettings.showExplanationsEnabled && rec.reason) {
                reasonEl.classList.remove('hidden');
                reasonEl.innerText = `💡 ${rec.reason}`;
            } else {
                reasonEl.classList.add('hidden');
            }
        }
        if (acceptBtn && rec.actionLabel) {
            acceptBtn.innerText = rec.actionLabel;
        }
    }

    let currentRoutineProposal = null;

    function openProposalModal(proposal) {
        currentRoutineProposal = proposal;
        const modal = document.getElementById('proposal-review-modal');
        const explText = document.getElementById('proposal-explanation-text');
        const countText = document.getElementById('proposal-count-text');
        const durText = document.getElementById('proposal-dur-text');
        const itemsList = document.getElementById('proposal-items-list');

        if (explText) explText.innerText = proposal.explanationMessage;
        if (countText) countText.innerText = `Itens propostos: ${proposal.items.length}`;
        if (durText) durText.innerText = `Tempo total: ${formatMinutesToHours(proposal.totalPlannedMinutes)}`;

        if (itemsList) {
            if (proposal.items.length === 0) {
                itemsList.innerHTML = `<div class="card" style="padding: 16px; text-align: center; color: var(--text-secondary);">Nenhuma atividade flexível pôde ser encaixada no tempo livre restante.</div>`;
            } else {
                itemsList.innerHTML = proposal.items.map((item, idx) => `
                    <div class="card timeline-card" style="padding: 12px; margin-bottom: 8px;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <span class="badge" style="background-color: rgba(99, 102, 241, 0.15); color: var(--primary-color); font-weight: 700; margin-bottom: 4px;">${item.startTime} - ${item.endTime} (${item.durationMinutes}m)</span>
                                <h4 style="font-size: 14px; font-weight: 700; margin: 4px 0 2px 0;">${escapeHtml(item.title)}</h4>
                                <span style="font-size: 11px; color: var(--text-secondary);">${item.category} • ${item.reason}</span>
                            </div>
                            <button class="btn-icon btn-proposal-remove" data-idx="${idx}" style="width: 28px; height: 28px;" title="Remover item da proposta">
                                <svg viewBox="0 0 24 24"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
                            </button>
                        </div>
                    </div>
                `).join('');

                itemsList.querySelectorAll('.btn-proposal-remove').forEach(btn => {
                    btn.addEventListener('click', () => {
                        const idx = parseInt(btn.getAttribute('data-idx'), 10);
                        proposal.items.splice(idx, 1);
                        proposal.totalPlannedMinutes = proposal.items.reduce((acc, c) => acc + c.durationMinutes, 0);
                        openProposalModal(proposal);
                    });
                });
            }
        }

        if (modal) modal.classList.remove('hidden');
    }

    function closeProposalModal() {
        const modal = document.getElementById('proposal-review-modal');
        if (modal) modal.classList.add('hidden');
        currentRoutineProposal = null;
    }

    // =========================================================
    // PLANNING SCREEN & WEEKLY NAV LOGIC (ETAPA 3)
    // =========================================================
    function getMonday(d) {
        const date = new Date(d);
        const day = date.getDay();
        const diff = date.getDate() - day + (day === 0 ? -6 : 1); // adjust when day is sunday
        return new Date(date.setDate(diff));
    }

    function renderWeekBar() {
        const monday = getMonday(selectedPlanningDate);
        const monthYearEl = document.getElementById('week-nav-month-year');
        const daysContainer = document.getElementById('week-days-container');

        const monthName = PT_MONTHS[monday.getMonth()];
        const monthCap = monthName.charAt(0).toUpperCase() + monthName.slice(1);
        if (monthYearEl) monthYearEl.innerText = `${monthCap} ${monday.getFullYear()}`;

        const today = new Date();
        const todayStr = toISODate(today);
        const selectedStr = toISODate(selectedPlanningDate);

        // Check which dates have scheduled tasks
        const allSchedules = Storage.getAllScheduleEntries();
        const scheduledDatesSet = new Set(allSchedules.filter(s => s.status !== 'CANCELLED').map(s => s.date));

        const days = [];
        for (let i = 0; i < 7; i++) {
            const current = new Date(monday);
            current.setDate(monday.getDate() + i);
            const dateStr = toISODate(current);
            const isSelected = dateStr === selectedStr;
            const isToday = dateStr === todayStr;
            const hasTask = scheduledDatesSet.has(dateStr);
            const dayLabel = PT_DAYS_SHORT[current.getDay()];

            days.push(`
                <div class="week-day-pill ${isSelected ? 'active' : ''} ${isToday ? 'is-today' : ''}" data-date="${dateStr}">
                    <span class="day-name">${dayLabel}</span>
                    <span class="day-num">${current.getDate()}</span>
                    <div class="week-day-dot ${hasTask ? 'has-task' : ''}"></div>
                </div>
            `);
        }

        if (daysContainer) {
            daysContainer.innerHTML = days.join('');
            daysContainer.querySelectorAll('.week-day-pill').forEach(pill => {
                pill.addEventListener('click', () => {
                    const dStr = pill.getAttribute('data-date');
                    const parts = dStr.split('-');
                    selectedPlanningDate = new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
                    refreshPlanning();
                });
            });
        }
    }

    function refreshPlanning() {
        renderWeekBar();

        const profile = Storage.getProfile();
        const activities = Storage.getActivities();
        const dateStr = toISODate(selectedPlanningDate);
        const scheduled = Storage.getScheduleEntries(dateStr);
        const fixedToday = activities.filter(a => a.type === 'FIXED' && isFixedActiveOnDate(a, selectedPlanningDate));

        // Date Label
        const dayLabelEl = document.getElementById('plan-current-day-label');
        if (dayLabelEl) dayLabelEl.innerText = formatDisplayDate(selectedPlanningDate);

        // Availability calculation
        const avail = calculateDayAvailability(selectedPlanningDate, profile.wakeUpTime, profile.sleepTime, activities.filter(a => a.type === 'FIXED'), scheduled);
        updateAvailabilityCardUI('plan', avail);

        // Timeline Container
        const timelineContainer = document.getElementById('planning-timeline-container');
        const emptyState = document.getElementById('planning-timeline-empty');

        const hasItems = fixedToday.length > 0 || scheduled.length > 0;

        if (timelineContainer && emptyState) {
            if (!hasItems) {
                emptyState.classList.remove('hidden');
                timelineContainer.innerHTML = '';
            } else {
                emptyState.classList.add('hidden');

                let html = '';

                // 1. Fixed Commitments Header & Items
                if (fixedToday.length > 0) {
                    html += `<div style="font-size: 13px; font-weight: 700; color: var(--text-secondary); margin-top: 6px;">Compromissos fixos</div>`;
                    fixedToday.forEach(f => {
                        html += renderFixedTimelineCardHtml(f);
                    });
                }

                // 2. Scheduled Activities
                if (scheduled.length > 0) {
                    html += `<div style="font-size: 13px; font-weight: 700; color: var(--text-secondary); margin-top: 14px;">Atividades planejadas (${scheduled.length})</div>`;
                    scheduled.forEach(e => {
                        html += renderScheduledCardHtml(e);
                    });
                }

                // 3. Free Slots Gaps
                if (avail.freeSlots.length > 0) {
                    html += `<div style="font-size: 13px; font-weight: 700; color: var(--text-secondary); margin-top: 14px;">Horários livres disponíveis</div>`;
                    avail.freeSlots.slice(0, 3).forEach(slot => {
                        const sH = Math.floor(slot.start / 60) % 24;
                        const sM = slot.start % 60;
                        const eH = Math.floor(slot.end / 60) % 24;
                        const eM = slot.end % 60;
                        const rangeStr = `${formatTime(sH, sM)} às ${formatTime(eH, eM)}`;
                        const durStr = formatMinutesToHours(slot.duration);

                        html += `
                            <div class="free-slot-card" data-start-h="${sH}" data-start-m="${sM}" data-dur="${slot.duration}">
                                <div class="free-slot-left">
                                    <svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>
                                    <span>${rangeStr} • ${durStr} livres</span>
                                </div>
                                <div class="free-slot-badge">
                                    <svg viewBox="0 0 24 24"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
                                    <span>Alocar</span>
                                </div>
                            </div>
                        `;
                    });
                }

                timelineContainer.innerHTML = html;
            }
        }

        setupTimelineEventListeners();
    }

    function renderFixedTimelineCardHtml(activity) {
        const startStr = formatTime(activity.startHour || 8, activity.startMinute || 0);
        const endStr = formatTime(activity.endHour || 10, activity.endMinute || 0);
        const startMin = (activity.startHour || 8) * 60 + (activity.startMinute || 0);
        const endMin = (activity.endHour || 10) * 60 + (activity.endMinute || 0);
        const crosses = endMin <= startMin;
        const durMin = crosses ? (1440 - startMin) + endMin : endMin - startMin;
        const durStr = formatMinutesToHours(durMin);
        const rangeText = crosses ? `${startStr} às ${endStr} (+1 dia)` : `${startStr} às ${endStr}`;

        return `
            <div class="scheduled-item-card" style="border-left: 4px solid #8B5CF6;">
                <div class="item-content-col" style="margin-left: 4px;">
                    <div class="item-time-row">
                        <span class="item-time-text" style="color: #8B5CF6;">${rangeText}</span>
                        <span class="item-status-pill" style="background-color: rgba(139, 92, 246, 0.15); color: #8B5CF6;">Fixo</span>
                    </div>
                    <div class="item-title">${activity.title}</div>
                    <div class="item-subtitle">${activity.category} • ${durStr}</div>
                </div>
            </div>
        `;
    }

    function renderScheduledCardHtml(entry, index = 0, totalCount = 1) {
        const isCompleted = entry.status === 'COMPLETED';
        const startStr = formatTime(entry.startHour, entry.startMinute);
        const endMin = entry.startHour * 60 + entry.startMinute + entry.targetDurationMinutes;
        const endH = Math.floor(endMin / 60) % 24;
        const endM = endMin % 60;
        const rangeStr = `${startStr} às ${formatTime(endH, endM)}`;
        const durStr = formatMinutesToHours(entry.targetDurationMinutes);

        const statusInfo = STATUS_INFO[entry.status] || STATUS_INFO.PENDING;

        return `
            <div class="scheduled-item-card timeline-card ${isCompleted ? 'completed' : ''}" draggable="true" data-id="${entry.id}" data-index="${index}">
                <div class="checklist-circle ${isCompleted ? 'completed' : ''}" data-id="${entry.id}" title="Marcar como concluída">
                    <svg viewBox="0 0 24 24"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                </div>
                <div class="item-content-col">
                    <div class="item-time-row">
                        <span class="item-time-text">${rangeStr}</span>
                        <span class="item-status-pill" style="background-color: ${statusInfo.bg}; color: ${statusInfo.color};">${statusInfo.label}</span>
                    </div>
                    <div class="item-title">${entry.activityTitle}</div>
                    <div class="item-subtitle">${entry.category} • ${durStr}</div>
                </div>
                <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; margin-right: 4px;">
                    <button type="button" class="reorder-btn btn-reorder-up" data-index="${index}" ${index === 0 ? 'disabled' : ''} title="Mover para cima">▲</button>
                    <button type="button" class="reorder-btn btn-reorder-down" data-index="${index}" ${index === totalCount - 1 ? 'disabled' : ''} title="Mover para baixo">▼</button>
                </div>
                <div style="display: flex; gap: 4px;">
                    <button class="btn-icon btn-sched-action" data-action="prompt" data-id="${entry.id}" title="Opções" style="width: 32px; height: 32px;">
                        <svg viewBox="0 0 24 24" style="width: 16px; height: 16px;"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
                    </button>
                </div>
            </div>
        `;
    }

    function reorderTodayScheduledEntries(fromIndex, toIndex) {
        const todayStr = toISODate(new Date());
        const entries = Storage.getScheduleEntries(todayStr);
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= entries.length || toIndex >= entries.length || fromIndex === toIndex) return;

        const [moved] = entries.splice(fromIndex, 1);
        entries.splice(toIndex, 0, moved);

        const activities = Storage.getActivities();
        const fixedToday = activities.filter(a => a.type === 'FIXED' && isFixedActiveOnDate(a, new Date()));
        const fixedIntervals = fixedToday.map(f => {
            const [sh, sm] = (f.startTime || '09:00').split(':').map(Number);
            const [eh, em] = (f.endTime || '10:00').split(':').map(Number);
            return { start: sh * 60 + sm, end: eh * 60 + em };
        }).sort((a, b) => a.start - b.start);

        let cursor = entries[0] ? (entries[0].startHour * 60 + entries[0].startMinute) : (8 * 60);
        cursor = Math.max(cursor, 8 * 60);

        entries.forEach(entry => {
            const dur = Math.max(entry.targetDurationMinutes || 30, 10);
            let valid = false;
            let attempts = 0;
            while (!valid && attempts < 20) {
                attempts++;
                const s = cursor;
                const e = cursor + dur;
                const conflict = fixedIntervals.find(f => s < f.end && e > f.start);
                if (conflict) {
                    cursor = conflict.end;
                } else {
                    valid = true;
                }
            }
            entry.startHour = Math.floor(cursor / 60) % 24;
            entry.startMinute = cursor % 60;
            const endTotal = cursor + dur;
            entry.endHour = Math.floor(endTotal / 60) % 24;
            entry.endMinute = endTotal % 60;
            cursor = endTotal + 5;
            Storage.updateScheduleEntry(entry);
        });

        refreshHome();
        refreshPlanning();
    }

    function setupTimelineEventListeners() {
        // Checklist Checkbox click with celebratory checkmark
        document.querySelectorAll('.checklist-circle').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const id = parseInt(btn.getAttribute('data-id'), 10);
                const all = Storage.getAllScheduleEntries();
                const entry = all.find(s => s.id === id);
                if (entry) {
                    if (entry.status === 'COMPLETED') {
                        Storage.updateScheduleStatus(id, 'PENDING');
                        refreshPlanning();
                        refreshHome();
                    } else {
                        btn.classList.add('celebrate');
                        setTimeout(() => {
                            openCompletionPromptModal(entry);
                        }, 250);
                    }
                }
            });
        });

        // Up / Down reorder button clicks
        document.querySelectorAll('.btn-reorder-up').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const idx = parseInt(btn.getAttribute('data-index'), 10);
                if (idx > 0) reorderTodayScheduledEntries(idx, idx - 1);
            });
        });

        document.querySelectorAll('.btn-reorder-down').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const idx = parseInt(btn.getAttribute('data-index'), 10);
                reorderTodayScheduledEntries(idx, idx + 1);
            });
        });

        // HTML5 Drag and drop reordering
        let draggedIndex = null;
        document.querySelectorAll('.timeline-card[draggable="true"]').forEach(card => {
            card.addEventListener('dragstart', (e) => {
                draggedIndex = parseInt(card.getAttribute('data-index'), 10);
                card.classList.add('dragging');
                e.dataTransfer.effectAllowed = 'move';
            });

            card.addEventListener('dragover', (e) => {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                card.classList.add('drag-over');
            });

            card.addEventListener('dragleave', () => {
                card.classList.remove('drag-over');
            });

            card.addEventListener('drop', (e) => {
                e.preventDefault();
                card.classList.remove('drag-over');
                const targetIndex = parseInt(card.getAttribute('data-index'), 10);
                if (draggedIndex !== null && targetIndex !== draggedIndex) {
                    reorderTodayScheduledEntries(draggedIndex, targetIndex);
                }
            });

            card.addEventListener('dragend', () => {
                card.classList.remove('dragging');
                document.querySelectorAll('.timeline-card').forEach(c => c.classList.remove('drag-over'));
            });
        });

        // Item action button (options menu)
        document.querySelectorAll('.btn-sched-action').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const id = parseInt(btn.getAttribute('data-id'), 10);
                const all = Storage.getAllScheduleEntries();
                const entry = all.find(s => s.id === id);
                if (entry) openCompletionPromptModal(entry);
            });
        });

        // Free slot "Alocar" click
        document.querySelectorAll('.free-slot-card').forEach(card => {
            card.addEventListener('click', () => {
                const sH = parseInt(card.getAttribute('data-start-h'), 10);
                const sM = parseInt(card.getAttribute('data-start-m'), 10);
                const dur = parseInt(card.getAttribute('data-dur'), 10);
                openScheduleEntrySheet(sH, sM, dur);
            });
        });
    }

    // =========================================================
    // SCHEDULE ENTRY MODAL BOTTOM SHEET (ETAPAS 3, 4 & 6)
    // =========================================================
    let schedActiveTab = 'my_acts';
    let schedSelectedPredef = null;
    let openedFromScheduleSheet = false;

    function openScheduleEntrySheet(suggestedH = 9, suggestedM = 0, suggestedDur = 60, preselectActivityId = null) {
        schedStartHour = suggestedH;
        schedStartMinute = suggestedM;
        schedDurationMinutes = suggestedDur > 0 ? suggestedDur : 60;
        schedSelectedPredef = null;

        const activities = Storage.getActivities();
        if (preselectActivityId != null) {
            schedActiveTab = 'my_acts';
            schedSelectedActivityId = preselectActivityId;
            const act = activities.find(a => a.id === preselectActivityId);
            if (act && act.estimatedDurationMinutes > 0) schedDurationMinutes = act.estimatedDurationMinutes;
        } else if (activities.length > 0) {
            schedActiveTab = 'my_acts';
            schedSelectedActivityId = activities[0].id;
            schedDurationMinutes = activities[0].estimatedDurationMinutes || suggestedDur;
        } else {
            schedActiveTab = 'quick_sugg';
            schedSelectedActivityId = null;
            schedSelectedPredef = PREDEFINED_LIBRARY[0];
            schedDurationMinutes = PREDEFINED_LIBRARY[0].duration;
        }

        // Setup tab switchers
        const tabMyActs = document.getElementById('sched-tab-my-acts');
        const tabQuickSugg = document.getElementById('sched-tab-quick-sugg');
        const btnCreateInline = document.getElementById('sched-btn-create-inline');

        if (tabMyActs) {
            tabMyActs.classList.toggle('active', schedActiveTab === 'my_acts');
            tabMyActs.onclick = () => {
                schedActiveTab = 'my_acts';
                tabMyActs.classList.add('active');
                if (tabQuickSugg) tabQuickSugg.classList.remove('active');
                renderSchedActivityChips();
                renderSchedSmartSlots();
            };
        }

        if (tabQuickSugg) {
            tabQuickSugg.classList.toggle('active', schedActiveTab === 'quick_sugg');
            tabQuickSugg.onclick = () => {
                schedActiveTab = 'quick_sugg';
                tabQuickSugg.classList.add('active');
                if (tabMyActs) tabMyActs.classList.remove('active');
                renderSchedActivityChips();
                renderSchedSmartSlots();
            };
        }

        if (btnCreateInline) {
            btnCreateInline.onclick = () => {
                closeScheduleEntrySheet();
                openedFromScheduleSheet = true;
                openAddActivitySheet('FLEXIBLE');
            };
        }

        renderSchedActivityChips();
        updateSchedTimeUI();
        updateSchedDurationUI();
        renderSchedSmartSlots();

        const sheet = document.getElementById('schedule-entry-sheet');
        if (sheet) sheet.classList.remove('hidden');
    }

    function renderSchedSmartSlots() {
        const container = document.getElementById('sched-smart-slots-chips');
        const reasonEl = document.getElementById('sched-smart-slots-reason');
        if (!container) return;

        const selectedDate = currentSelectedDate || new Date();
        const profile = Storage.getProfile();
        const fixedActs = Storage.getActivities().filter(a => a.type === 'FIXED');
        const existingEntries = Storage.getScheduleEntriesForDate(selectedDate);
        const avail = calculateDayAvailability(selectedDate, profile.wakeUpTime, profile.sleepTime, fixedActs, existingEntries);

        const isToday = toISODate(selectedDate) === toISODate(new Date());
        const nowMinutes = isToday ? (new Date().getHours() * 60 + new Date().getMinutes()) : 0;
        const dur = schedDurationMinutes || 30;

        const records = Storage.getExecutionRecords ? Storage.getExecutionRecords() : [];
        const actRecords = schedSelectedActivityId ? records.filter(r => r.activityId === schedSelectedActivityId) : [];

        const options = [];
        for (const slot of avail.freeSlots) {
            const effStart = isToday ? Math.max(slot.startMinute, nowMinutes) : slot.startMinute;
            const gap = slot.endMinute - effStart;
            if (gap >= dur) {
                const sH = Math.floor(effStart / 60) % 24;
                const sM = effStart % 60;
                const eMin = effStart + dur;
                const eH = Math.floor(eMin / 60) % 24;
                const eM = eMin % 60;

                const slotHour = sH;
                const attemptsInSlot = actRecords.filter(r => Math.abs(r.plannedStartHour - slotHour) <= 1);
                const completedInSlot = attemptsInSlot.filter(r => r.finalStatus === 'COMPLETED');
                const isRec = attemptsInSlot.length >= 3 && (completedInSlot.length / attemptsInSlot.length) >= 0.6;

                options.push({
                    startHour: sH,
                    startMinute: sM,
                    rangeText: `${formatTime(sH, sM)} - ${formatTime(eH, eM)}`,
                    isRecommended: isRec,
                    reason: isRec ? 'Você costuma concluir suas tarefas com mais frequência nesse horário.' : 'Horário livre sugerido com base na sua rotina.'
                });

                if (gap >= dur + 60) {
                    const sH2 = Math.floor((effStart + 60) / 60) % 24;
                    const sM2 = (effStart + 60) % 60;
                    const eMin2 = effStart + 60 + dur;
                    const eH2 = Math.floor(eMin2 / 60) % 24;
                    const eM2 = eMin2 % 60;
                    options.push({
                        startHour: sH2,
                        startMinute: sM2,
                        rangeText: `${formatTime(sH2, sM2)} - ${formatTime(eH2, eM2)}`,
                        isRecommended: false,
                        reason: 'Horário livre sugerido com base na sua rotina.'
                    });
                }
            }
        }

        if (options.length === 0) {
            container.innerHTML = `<span style="font-size: 12px; color: var(--text-secondary);">Nenhum horário livre contínuo de ${dur}m disponível hoje. Ajuste a duração ou escolha manualmente abaixo.</span>`;
            if (reasonEl) reasonEl.innerText = '';
            return;
        }

        options.sort((a, b) => (b.isRecommended ? 1 : 0) - (a.isRecommended ? 1 : 0));
        const topOptions = options.slice(0, 3);

        container.innerHTML = topOptions.map((opt, i) => {
            const isSel = opt.startHour === schedStartHour && opt.startMinute === schedStartMinute;
            const badge = opt.isRecommended ? '<span style="color: #10B981; font-weight: 700; font-size: 10px; margin-left: 4px;">⭐ Recomendado</span>' : '<span style="color: var(--primary-color); font-size: 10px; margin-left: 4px;">Livre</span>';
            return `<div class="filter-chip sched-slot-chip ${isSel ? 'selected' : ''}" data-idx="${i}" style="cursor: pointer; padding: 6px 10px; font-size: 12px;">
                ${opt.rangeText} ${badge}
            </div>`;
        }).join('');

        container.querySelectorAll('.sched-slot-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const idx = parseInt(chip.getAttribute('data-idx'), 10);
                const opt = topOptions[idx];
                schedStartHour = opt.startHour;
                schedStartMinute = opt.startMinute;
                updateSchedTimeUI();
                renderSchedSmartSlots();
                if (reasonEl) reasonEl.innerText = opt.reason;
            });
        });

        const currentOpt = topOptions.find(o => o.startHour === schedStartHour && o.startMinute === schedStartMinute);
        if (reasonEl && currentOpt) {
            reasonEl.innerText = currentOpt.reason;
        } else if (reasonEl) {
            reasonEl.innerText = 'Toque em uma das opções acima para preencher o horário ou ajuste manualmente abaixo.';
        }
    }

    function closeScheduleEntrySheet() {
        const sheet = document.getElementById('schedule-entry-sheet');
        if (sheet) sheet.classList.add('hidden');
    }

    function renderSchedActivityChips() {
        const container = document.getElementById('sched-activities-chips');
        if (!container) return;

        if (schedActiveTab === 'my_acts') {
            const activities = Storage.getActivities();
            if (activities.length === 0) {
                container.innerHTML = `
                    <div style="text-align: center; width: 100%; padding: 12px; font-size: 13px; color: var(--text-secondary);">
                        Você ainda não cadastrou atividades. Toque no botão acima para criar sua primeira atividade.
                    </div>
                `;
                return;
            }

            container.innerHTML = activities.map(a => `
                <div class="filter-chip sched-act-chip ${a.id === schedSelectedActivityId ? 'selected' : ''}" data-id="${a.id}" data-dur="${a.estimatedDurationMinutes}">
                    ${a.title} (${a.estimatedDurationMinutes}m)
                </div>
            `).join('');

            container.querySelectorAll('.sched-act-chip').forEach(chip => {
                chip.addEventListener('click', () => {
                    schedSelectedActivityId = parseInt(chip.getAttribute('data-id'), 10);
                    schedSelectedPredef = null;
                    const dur = parseInt(chip.getAttribute('data-dur'), 10);
                    if (dur > 0) schedDurationMinutes = dur;
                    renderSchedActivityChips();
                    updateSchedDurationUI();
                });
            });
        } else {
            // Quick suggestions library
            container.innerHTML = PREDEFINED_LIBRARY.map((p, idx) => `
                <div class="filter-chip sched-predef-chip ${schedSelectedPredef && schedSelectedPredef.title === p.title ? 'selected' : ''}" data-index="${idx}">
                    ${p.title} (${p.category} • ${p.duration}m)
                </div>
            `).join('');

            container.querySelectorAll('.sched-predef-chip').forEach(chip => {
                chip.addEventListener('click', () => {
                    const idx = parseInt(chip.getAttribute('data-index'), 10);
                    schedSelectedPredef = PREDEFINED_LIBRARY[idx];
                    schedSelectedActivityId = null;
                    schedDurationMinutes = schedSelectedPredef.duration;
                    renderSchedActivityChips();
                    updateSchedDurationUI();
                });
            });
        }
    }

    function updateSchedTimeUI() {
        const timeDisplay = document.getElementById('sched-time-display');
        if (timeDisplay) timeDisplay.innerText = formatTime(schedStartHour, schedStartMinute);
        checkSchedConflicts();
    }

    function updateSchedDurationUI() {
        document.querySelectorAll('.sched-dur-chip').forEach(chip => {
            const mins = parseInt(chip.getAttribute('data-mins'), 10);
            chip.classList.toggle('selected', mins === schedDurationMinutes);
        });

        const durDisplay = document.getElementById('sched-duration-display');
        if (durDisplay) durDisplay.innerText = formatMinutesToHours(schedDurationMinutes);
        checkSchedConflicts();
    }

    function checkSchedConflicts() {
        const textEl = document.getElementById('sched-feedback-text');
        const cardEl = document.getElementById('sched-feedback-card');
        const iconEl = document.getElementById('sched-feedback-icon');

        const sMin = schedStartHour * 60 + schedStartMinute;
        const eMin = sMin + schedDurationMinutes;
        const eH = Math.floor(eMin / 60) % 24;
        const eM = eMin % 60;
        const rangeText = `${formatTime(schedStartHour, schedStartMinute)} às ${formatTime(eH, eM)} (${formatMinutesToHours(schedDurationMinutes)})`;

        // Check if overlaps any existing entry or fixed commitment on selected planning date
        const dateStr = toISODate(selectedPlanningDate);
        const profile = Storage.getProfile();
        const activities = Storage.getActivities();
        const scheduled = Storage.getScheduleEntries(dateStr);
        const avail = calculateDayAvailability(selectedPlanningDate, profile.wakeUpTime, profile.sleepTime, activities.filter(a => a.type === 'FIXED'), scheduled);

        const proposedSlot = { start: sMin, end: eMin, label: 'Nova atividade' };
        const directConflict = avail.conflicts.find(c => c.start < proposedSlot.end && proposedSlot.start < c.end);

        if (directConflict && textEl && cardEl) {
            cardEl.style.borderColor = 'var(--error-color)';
            if (iconEl) iconEl.style.fill = 'var(--error-color)';
            textEl.innerText = `Atenção: Horário sobreposto com "${directConflict.titleA}" ou "${directConflict.titleB}".`;
        } else if (textEl && cardEl) {
            cardEl.style.borderColor = 'var(--primary-color)';
            if (iconEl) iconEl.style.fill = 'var(--primary-color)';
            textEl.innerText = `Horário: ${rangeText}`;
        }
    }

    function confirmScheduleEntry() {
        let targetActivity = null;

        if (schedSelectedActivityId) {
            const activities = Storage.getActivities();
            targetActivity = activities.find(a => a.id === schedSelectedActivityId);
        } else if (schedSelectedPredef) {
            // Persist library activity to user storage so it appears under "Minhas atividades"
            const newActId = Storage.saveActivity({
                title: schedSelectedPredef.title,
                category: schedSelectedPredef.category,
                type: 'FLEXIBLE',
                estimatedDurationMinutes: schedDurationMinutes
            });
            targetActivity = Storage.getActivities().find(a => a.id === newActId);
        }

        if (!targetActivity) {
            alert('Por favor, selecione uma atividade para agendar.');
            return;
        }

        const dateStr = toISODate(selectedPlanningDate);
        const now = new Date();
        const todayStr = now.toISOString().split('T')[0];
        const nowMin = now.getHours() * 60 + now.getMinutes();
        const proposedStart = schedStartHour * 60 + schedStartMinute;

        if (dateStr === todayStr && proposedStart < nowMin) {
            alert('Esse horário já passou. Por favor, escolha um horário a partir de agora.');
            return;
        }

        const entry = {
            id: Date.now(),
            activityId: targetActivity.id,
            activityTitle: targetActivity.title,
            category: targetActivity.category,
            date: dateStr,
            startHour: schedStartHour,
            startMinute: schedStartMinute,
            endHour: (schedStartHour + Math.floor((schedStartMinute + schedDurationMinutes) / 60)) % 24,
            endMinute: (schedStartMinute + schedDurationMinutes) % 60,
            targetDurationMinutes: schedDurationMinutes,
            status: 'PENDING'
        };

        Storage.saveScheduleEntry(entry);
        closeScheduleEntrySheet();
        refreshPlanning();
        refreshHome();
    }

    // =========================================================
    // COMPLETION PROMPT & POSTPONE MODALS (ETAPA 3)
    // =========================================================
    function openCompletionPromptModal(entry) {
        activePromptEntryId = entry.id;
        const modal = document.getElementById('completion-prompt-modal');
        const descEl = document.getElementById('completion-modal-desc');

        const dur = formatMinutesToHours(entry.targetDurationMinutes);
        if (descEl) descEl.innerText = `A atividade "${entry.activityTitle}" estava planejada para ${dur}. Como foi a execução?`;

        // Contextual Checklist (Etapa 4)
        const ctx = Storage.getContextForActivity(entry.activityId);
        const ctxBox = document.getElementById('completion-context-box');
        const btnAdvance = document.getElementById('btn-complete-advance');

        if (ctx && ctx.progressLabel) {
            const currentLabel = ctx.progressLabel;
            const nextUnit = (ctx.currentUnit || 1) + 1;
            let nextLabel = ctx.contentType === 'SERIES' ? `Episódio ${nextUnit}` : (ctx.contentType === 'BOOK' ? `Capítulo ${nextUnit}` : `Módulo ${nextUnit}`);
            
            const curEl = document.getElementById('comp-ctx-current');
            const nextEl = document.getElementById('comp-ctx-next');
            if (curEl) curEl.innerText = currentLabel;
            if (nextEl) nextEl.innerText = nextLabel;

            if (ctxBox) ctxBox.classList.remove('hidden');
            if (btnAdvance) {
                btnAdvance.classList.remove('hidden');
                btnAdvance.innerText = `Concluir e avançar para o ${nextLabel}`;
            }
        } else {
            if (ctxBox) ctxBox.classList.add('hidden');
            if (btnAdvance) btnAdvance.classList.add('hidden');
        }

        if (modal) modal.classList.remove('hidden');
    }

    function closeCompletionPromptModal() {
        const modal = document.getElementById('completion-prompt-modal');
        if (modal) modal.classList.add('hidden');
    }

    function openPostponeModal() {
        closeCompletionPromptModal();
        const modal = document.getElementById('postpone-entry-modal');
        const descEl = document.getElementById('postpone-modal-desc');

        const all = Storage.getAllScheduleEntries();
        const entry = all.find(s => s.id === activePromptEntryId);
        if (descEl && entry) {
            descEl.innerText = `A atividade "${entry.activityTitle}" não foi concluída. Seu planejamento pode mudar de forma natural. O que deseja fazer?`;
        }

        if (modal) modal.classList.remove('hidden');
    }

    function closePostponeModal() {
        const modal = document.getElementById('postpone-entry-modal');
        if (modal) modal.classList.add('hidden');
    }

    // =========================================================
    // ACTIVITIES TAB LOGIC & CARDS
    // =========================================================
    function refreshActivities() {
        const activities = Storage.getActivities();
        const container = document.getElementById('activities-list-container');
        const emptyState = document.getElementById('activities-empty-state');

        let filtered = activities.filter(a => {
            const matchesQuery = !activitySearchQuery ||
                fuzzyContains(a.title, activitySearchQuery) ||
                fuzzyContains(a.category, activitySearchQuery) ||
                fuzzyContains(a.description || '', activitySearchQuery);

            const matchesCategory = !activityCategoryFilter ||
                a.category.toLowerCase() === activityCategoryFilter.toLowerCase();

            const matchesType = !activityTypeFilter || a.type === activityTypeFilter;

            return matchesQuery && matchesCategory && matchesType;
        });

        if (container && emptyState) {
            if (filtered.length > 0) {
                emptyState.classList.add('hidden');
                container.classList.remove('hidden');
                container.innerHTML = filtered.map(a => renderActivityCardHtml(a)).join('');
            } else {
                emptyState.classList.remove('hidden');
                container.classList.add('hidden');
                container.innerHTML = '';
            }
        }

        setupActivityCardActionListeners();
    }

    function renderActivityCardHtml(activity) {
        const catColor = CATEGORY_COLORS[activity.category] || '#8B9CFF';
        const typeLabel = activity.type === 'FLEXIBLE' ? 'Flexível' : (activity.type === 'FIXED' ? 'Fixo' : 'Meta semanal');

        let footerHtml = '';
        if (activity.type === 'FLEXIBLE') {
            const count = activity.defaultCount || 1;
            const dur = formatMinutesToHours(activity.estimatedDurationMinutes);
            const total = formatMinutesToHours(activity.estimatedDurationMinutes * count);
            const text = count > 1 ? `${count}x ${dur} (${total} total)` : dur;
            footerHtml = `<span><svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>${text}</span>`;
        } else if (activity.type === 'FIXED') {
            const startStr = formatTime(activity.startHour || 8, activity.startMinute || 0);
            const endStr = formatTime(activity.endHour || 10, activity.endMinute || 0);
            const startMin = (activity.startHour || 8) * 60 + (activity.startMinute || 0);
            const endMin = (activity.endHour || 10) * 60 + (activity.endMinute || 0);
            const crosses = endMin <= startMin;
            const durMin = crosses ? (1440 - startMin) + endMin : endMin - startMin;
            const durStr = formatMinutesToHours(durMin);
            const rangeText = crosses ? `${startStr} às ${endStr} (+1 dia, ${durStr})` : `${startStr} às ${endStr} (${durStr})`;

            footerHtml = `<span><svg viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>${rangeText}</span>
            <span class="activity-recurrence-tag"><svg viewBox="0 0 24 24"><path d="M19 8l-4 4h3c0 3.31-2.69 6-6 6-1.01 0-1.97-.25-2.8-.7l-1.46 1.46C8.97 19.54 10.43 20 12 20c4.42 0 8-3.58 8-8h3l-4-4zM6 12c0-3.31 2.69-6 6-6 1.01 0 1.97.25 2.8.7l1.46-1.46C15.03 4.46 13.57 4 12 4c-4.42 0-8 3.58-8 8H1l4 4 4-4H6z"/></svg>${activity.recurrenceType === 'WEEKDAYS' ? 'Dias úteis' : (activity.recurrenceType === 'ONCE' ? 'Uma vez' : 'Diário')}</span>`;
        } else if (activity.type === 'HABIT') {
            const target = activity.weeklyTargetCount || 3;
            const dur = formatMinutesToHours(activity.estimatedDurationMinutes);
            footerHtml = `<span><svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>${target}x por semana • ${dur}</span>`;
        }

        return `
        <div class="activity-card" data-id="${activity.id}">
            <div class="activity-card-header">
                <div class="activity-card-badges">
                    <span class="category-pill" style="background-color: ${catColor}22; color: ${catColor};">
                        <span class="category-dot" style="background-color: ${catColor};"></span>
                        ${activity.category}
                    </span>
                    <span class="activity-type-pill">${typeLabel}</span>
                </div>
                <div class="activity-card-actions">
                    <button class="btn-icon btn-card-action" data-action="edit" data-id="${activity.id}" title="Editar" style="width: 32px; height: 32px;">
                        <svg viewBox="0 0 24 24" style="width: 16px; height: 16px;"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
                    </button>
                    <button class="btn-icon btn-card-action" data-action="duplicate" data-id="${activity.id}" title="Duplicar" style="width: 32px; height: 32px;">
                        <svg viewBox="0 0 24 24" style="width: 16px; height: 16px;"><path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg>
                    </button>
                    <button class="btn-icon btn-card-action" data-action="delete" data-id="${activity.id}" title="Excluir" style="width: 32px; height: 32px; color: var(--error-color);">
                        <svg viewBox="0 0 24 24" style="width: 16px; height: 16px;"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
                    </button>
                </div>
            </div>
            <div class="activity-card-title">${activity.title}</div>
            ${activity.description ? `<div class="activity-card-desc">${activity.description}</div>` : ''}
            <div class="activity-card-footer">${footerHtml}</div>
        </div>`;
    }

    function setupActivityCardActionListeners() {
        const actionBtns = document.querySelectorAll('.btn-card-action');
        actionBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const action = btn.getAttribute('data-action');
                const id = parseInt(btn.getAttribute('data-id'), 10);

                if (action === 'edit') openEditActivity(id);
                else if (action === 'duplicate') {
                    Storage.duplicateActivity(id);
                    refreshAllTabs();
                } else if (action === 'delete') promptDeleteActivity(id);
            });
        });
    }

    function refreshAllTabs() {
        refreshHome();
        refreshActivities();
        refreshPlanning();
    }

    // =========================================================
    // ADD / EDIT ACTIVITY SHEET LOGIC
    // =========================================================
    function openAddActivitySheet(type = 'FLEXIBLE', existingActivity = null) {
        editingActivityId = existingActivity ? existingActivity.id : null;
        sheetType = existingActivity ? existingActivity.type : type;
        sheetCategory = existingActivity ? existingActivity.category : 'Casa';
        sheetDurationMinutes = existingActivity ? existingActivity.estimatedDurationMinutes : 30;
        sheetCount = existingActivity ? (existingActivity.defaultCount || 1) : 1;
        sheetStartHour = existingActivity ? (existingActivity.startHour || 8) : 8;
        sheetStartMinute = existingActivity ? (existingActivity.startMinute || 0) : 0;
        sheetEndHour = existingActivity ? (existingActivity.endHour || 10) : 10;
        sheetEndMinute = existingActivity ? (existingActivity.endMinute || 0) : 0;
        sheetRecurrence = existingActivity ? (existingActivity.recurrenceType || 'DAILY') : 'DAILY';
        sheetWeeklyTarget = existingActivity ? (existingActivity.weeklyTargetCount || 3) : 3;

        const sheetTitleEl = document.getElementById('sheet-title');
        if (sheetTitleEl) sheetTitleEl.innerText = existingActivity ? 'Editar Atividade' : 'Nova Atividade';

        const titleInput = document.getElementById('sheet-input-title');
        if (titleInput) titleInput.value = existingActivity ? existingActivity.title : '';

        const descInput = document.getElementById('sheet-input-desc');
        if (descInput) descInput.value = existingActivity ? (existingActivity.description || '') : '';

        updateSheetTypeUI();
        updateSheetCategoryUI();
        updateSheetDurationUI();
        updateSheetCountUI();
        updateSheetFixedTimeUI();
        updateSheetWeeklyTargetUI();
        updateSheetSuggestions();

        const sheet = document.getElementById('add-activity-sheet');
        if (sheet) sheet.classList.remove('hidden');
    }

    function closeAddActivitySheet() {
        const sheet = document.getElementById('add-activity-sheet');
        if (sheet) sheet.classList.add('hidden');
        const pill = document.getElementById('sheet-context-pill');
        if (pill) pill.classList.add('hidden');
        editingActivityId = null;
    }

    function openEditActivity(id) {
        const activity = Storage.getActivities().find(a => a.id === id);
        if (activity) openAddActivitySheet(activity.type, activity);
    }

    function updateSheetTypeUI() {
        document.querySelectorAll('.sheet-type-chip').forEach(chip => {
            const t = chip.getAttribute('data-type');
            chip.classList.toggle('selected', t === sheetType);
        });

        const flexibleSection = document.getElementById('sheet-flexible-section');
        const fixedSection = document.getElementById('sheet-fixed-section');
        const habitSection = document.getElementById('sheet-habit-section');

        if (flexibleSection) flexibleSection.classList.toggle('hidden', sheetType !== 'FLEXIBLE');
        if (fixedSection) fixedSection.classList.toggle('hidden', sheetType !== 'FIXED');
        if (habitSection) habitSection.classList.toggle('hidden', sheetType !== 'HABIT');
    }

    function updateSheetCategoryUI() {
        document.querySelectorAll('.sheet-category-chip').forEach(chip => {
            const cat = chip.getAttribute('data-category');
            chip.classList.toggle('selected', cat === sheetCategory);
        });
    }

    function updateSheetDurationUI() {
        document.querySelectorAll('.sheet-dur-chip').forEach(chip => {
            const mins = parseInt(chip.getAttribute('data-mins'), 10);
            chip.classList.toggle('selected', mins === sheetDurationMinutes);
        });

        const durLabel = document.getElementById('sheet-duration-display');
        if (durLabel) durLabel.innerText = formatMinutesToHours(sheetDurationMinutes);

        updateSheetCountUI();
    }

    function updateSheetCountUI() {
        const countValEl = document.getElementById('sheet-count-val');
        const totalDurEl = document.getElementById('sheet-total-dur-display');

        if (countValEl) countValEl.innerText = sheetCount;
        if (totalDurEl) {
            const total = sheetDurationMinutes * sheetCount;
            totalDurEl.innerText = `${sheetCount}x ${formatMinutesToHours(sheetDurationMinutes)} = ${formatMinutesToHours(total)} total`;
        }
    }

    function updateSheetFixedTimeUI() {
        const startBtn = document.getElementById('sheet-start-btn');
        const endBtn = document.getElementById('sheet-end-btn');
        const durBadge = document.getElementById('sheet-fixed-dur-badge');

        const startStr = formatTime(sheetStartHour, sheetStartMinute);
        const endStr = formatTime(sheetEndHour, sheetEndMinute);

        if (startBtn) startBtn.innerText = startStr;
        if (endBtn) endBtn.innerText = endStr;

        const startMin = sheetStartHour * 60 + sheetStartMinute;
        const endMin = sheetEndHour * 60 + sheetEndMinute;
        const crosses = endMin <= startMin;
        const durMin = crosses ? (1440 - startMin) + endMin : endMin - startMin;

        if (durBadge) {
            durBadge.innerText = crosses
                ? `Duração: ${formatMinutesToHours(durMin)} (+1 dia)`
                : `Duração: ${formatMinutesToHours(durMin)}`;
        }

        document.querySelectorAll('.sheet-rec-chip').forEach(chip => {
            const rec = chip.getAttribute('data-rec');
            chip.classList.toggle('selected', rec === sheetRecurrence);
        });
    }

    function updateSheetWeeklyTargetUI() {
        const targetValEl = document.getElementById('sheet-weekly-target-val');
        if (targetValEl) targetValEl.innerText = sheetWeeklyTarget;
    }

    function updateSheetSuggestions() {
        const titleInput = document.getElementById('sheet-input-title');
        const query = titleInput ? titleInput.value.trim() : '';
        const container = document.getElementById('sheet-suggestions-container');
        if (!container) return;

        let suggestions = PREDEFINED_LIBRARY;
        if (query) {
            suggestions = PREDEFINED_LIBRARY.filter(s =>
                fuzzyContains(s.title, query) || fuzzyContains(s.category, query)
            );
        }

        if (suggestions.length > 0 && query.length < 15) {
            container.innerHTML = suggestions.slice(0, 5).map(s => `
                <div class="onboarding-chip sheet-suggestion-chip" data-title="${s.title}" data-category="${s.category}" data-dur="${s.duration}" data-type="${s.type}">
                    <span>${s.title} (${s.duration}m)</span>
                </div>
            `).join('');

            container.querySelectorAll('.sheet-suggestion-chip').forEach(chip => {
                chip.addEventListener('click', () => {
                    const title = chip.getAttribute('data-title');
                    const cat = chip.getAttribute('data-category');
                    const dur = parseInt(chip.getAttribute('data-dur'), 10);
                    const type = chip.getAttribute('data-type');

                    if (titleInput) titleInput.value = title;
                    sheetCategory = cat;
                    sheetDurationMinutes = dur;
                    sheetType = type;

                    updateSheetTypeUI();
                    updateSheetCategoryUI();
                    updateSheetDurationUI();
                    updateSheetSuggestions();
                });
            });
        } else {
            container.innerHTML = '';
        }

        // LifeFlow Context detection (Etapa 4)
        const detected = ContextExtractor.extract(query, '', sheetCategory);
        const pill = document.getElementById('sheet-context-pill');
        const pillTitle = document.getElementById('sheet-context-pill-title');
        if (detected && pill && pillTitle) {
            pill.classList.remove('hidden');
            pillTitle.innerText = `Detectamos: ${detected.contentName} (${detected.progressLabel})`;
        } else if (pill) {
            pill.classList.add('hidden');
        }
    }

    function saveSheetActivity() {
        const titleInput = document.getElementById('sheet-input-title');
        const title = titleInput ? titleInput.value.trim() : '';
        if (!title) {
            alert('Por favor, informe o nome da atividade.');
            return;
        }

        const descInput = document.getElementById('sheet-input-desc');
        const description = descInput ? descInput.value.trim() : '';

        const activity = {
            id: editingActivityId || Date.now(),
            title: title,
            category: sheetCategory,
            type: sheetType,
            estimatedDurationMinutes: sheetDurationMinutes,
            defaultCount: sheetType === 'FLEXIBLE' ? sheetCount : 1,
            description: description,
            startHour: sheetType === 'FIXED' ? sheetStartHour : null,
            startMinute: sheetType === 'FIXED' ? sheetStartMinute : null,
            endHour: sheetType === 'FIXED' ? sheetEndHour : null,
            endMinute: sheetType === 'FIXED' ? sheetEndMinute : null,
            recurrenceType: sheetType === 'FIXED' ? sheetRecurrence : null,
            recurrenceDays: sheetType === 'FIXED' && sheetRecurrence === 'WEEKDAYS' ? [1, 2, 3, 4, 5] : [],
            weeklyTargetCount: sheetType === 'HABIT' ? sheetWeeklyTarget : null,
            colorHex: CATEGORY_COLORS[sheetCategory] || '#8B9CFF'
        };

        const saved = Storage.saveActivity(activity);
        const detected = ContextExtractor.extract(title, description, sheetCategory);
        if (detected) {
            Storage.saveContext({
                activityId: saved.id,
                ...detected,
                continuityStatus: 'IN_PROGRESS'
            });
        }

        closeAddActivitySheet();
        refreshAllTabs();

        if (openedFromScheduleSheet) {
            openedFromScheduleSheet = false;
            openScheduleEntrySheet(9, 0, saved.estimatedDurationMinutes || 30, saved.id);
        }
    }

    // --- Delete Confirmation Modal ---
    function promptDeleteActivity(id) {
        activityToDeleteId = id;
        const modal = document.getElementById('delete-confirm-modal');
        if (modal) modal.classList.remove('hidden');
    }

    function closeDeleteModal() {
        const modal = document.getElementById('delete-confirm-modal');
        if (modal) modal.classList.add('hidden');
        activityToDeleteId = null;
    }

    function confirmDeleteActivity() {
        if (activityToDeleteId) {
            Storage.deleteActivity(activityToDeleteId);
            closeDeleteModal();
            refreshAllTabs();
        }
    }

    // --- Time Picker Modal ---
    function openTimePicker(initialTimeStr, title = 'Selecionar horário', callback) {
        timePickerCallback = callback;
        const parsed = parseTime(initialTimeStr);
        pickerHour = parsed.hour;
        pickerMinute = parsed.minute;

        const modal = document.getElementById('time-picker-modal');
        const titleEl = document.getElementById('time-picker-title');
        const hourEl = document.getElementById('time-picker-hour-val');
        const minEl = document.getElementById('time-picker-min-val');

        if (titleEl) titleEl.innerText = title;
        if (hourEl) hourEl.innerText = String(pickerHour).padStart(2, '0');
        if (minEl) minEl.innerText = String(pickerMinute).padStart(2, '0');

        if (modal) modal.classList.remove('hidden');
    }

    function closeTimePicker() {
        const modal = document.getElementById('time-picker-modal');
        if (modal) modal.classList.add('hidden');
        timePickerCallback = null;
    }

    function confirmTimePicker() {
        if (timePickerCallback) {
            const timeStr = formatTime(pickerHour, pickerMinute);
            timePickerCallback(timeStr, pickerHour, pickerMinute);
        }
        closeTimePicker();
    }

    function incrementHour() {
        pickerHour = (pickerHour + 1) % 24;
        const hourEl = document.getElementById('time-picker-hour-val');
        if (hourEl) hourEl.innerText = String(pickerHour).padStart(2, '0');
    }

    function decrementHour() {
        pickerHour = (pickerHour + 23) % 24;
        const hourEl = document.getElementById('time-picker-hour-val');
        if (hourEl) hourEl.innerText = String(pickerHour).padStart(2, '0');
    }

    function incrementMinute() {
        pickerMinute = (pickerMinute + 5) % 60;
        const minEl = document.getElementById('time-picker-min-val');
        if (minEl) minEl.innerText = String(pickerMinute).padStart(2, '0');
    }

    function decrementMinute() {
        pickerMinute = (pickerMinute + 55) % 60;
        const minEl = document.getElementById('time-picker-min-val');
        if (minEl) minEl.innerText = String(pickerMinute).padStart(2, '0');
    }

    // --- Settings Screen Logic ---
    function refreshSettings() {
        const profile = Storage.getProfile();
        const nameInput = document.getElementById('settings-input-name');
        const wakeBtn = document.getElementById('settings-wake-btn');
        const sleepBtn = document.getElementById('settings-sleep-btn');

        if (nameInput && document.activeElement !== nameInput) {
            nameInput.value = profile.name || '';
        }

        if (wakeBtn) wakeBtn.innerText = profile.wakeUpTime;
        if (sleepBtn) sleepBtn.innerText = profile.sleepTime;

        document.querySelectorAll('input[name="theme-option"]').forEach(radio => {
            radio.checked = radio.value === profile.themeMode;
        });

        document.querySelectorAll('input[name="org-option"]').forEach(radio => {
            radio.checked = radio.value === profile.organizationMode;
        });

        document.querySelectorAll('input[name="balance-option"]').forEach(radio => {
            radio.checked = radio.value === (profile.balancePreference || 'BALANCED');
        });

        const learningSettings = Storage.getLearningSettings();
        const toggleRec = document.getElementById('setting-toggle-recommendations');
        const toggleLearn = document.getElementById('setting-toggle-learning');
        const toggleExp = document.getElementById('setting-toggle-explanations');

        if (toggleRec) toggleRec.checked = learningSettings.personalizedRecommendationsEnabled;
        if (toggleLearn) toggleLearn.checked = learningSettings.learningEnabled;
        if (toggleExp) toggleExp.checked = learningSettings.showExplanationsEnabled;

        const notifSettings = Storage.getNotificationSettings();
        const toggleRem = document.getElementById('setting-toggle-reminders');
        const toggleBefore = document.getElementById('setting-toggle-notify-before');
        const toggleComp = document.getElementById('setting-toggle-completion-prompt');
        const toggleQuiet = document.getElementById('setting-toggle-quiet-hours');
        const toggleConsent = document.getElementById('setting-toggle-usage-consent');

        if (toggleRem) toggleRem.checked = notifSettings.remindersEnabled;
        if (toggleBefore) toggleBefore.checked = notifSettings.notifyBeforeStart;
        if (toggleComp) toggleComp.checked = notifSettings.promptCompletionOnEnd;
        if (toggleQuiet) toggleQuiet.checked = notifSettings.quietHoursEnabled;
        if (toggleConsent) toggleConsent.checked = notifSettings.usageMonitoringConsentGranted;
    }

    // =========================================================
    // HABITS & LEARNING DASHBOARD (Etapa 5 / Seção 12)
    // =========================================================
    function refreshHabits() {
        const records = Storage.getExecutionHistory();
        const activities = Storage.getActivities();
        const scheduledToday = Storage.getScheduleEntries(toISODate(new Date()));
        const summary = HabitLearningEngine.computeSummary(records, activities, scheduledToday);

        updateDayBalanceUI();

        const emptyEl = document.getElementById('habits-empty-state');
        const contentEl = document.getElementById('habits-dashboard-content');

        if (!summary.hasSufficientData) {
            if (emptyEl) emptyEl.classList.remove('hidden');
            if (contentEl) contentEl.classList.add('hidden');
            return;
        }

        if (emptyEl) emptyEl.classList.add('hidden');
        if (contentEl) contentEl.classList.remove('hidden');

        // 1. Consistency
        const rate = summary.totalRecords > 0 ? Math.round((summary.completedCount / summary.totalRecords) * 100) : 0;
        const rateEl = document.getElementById('habits-consistency-rate');
        const barEl = document.getElementById('habits-consistency-bar');
        const textEl = document.getElementById('habits-consistency-text');

        if (rateEl) rateEl.innerText = `${rate}% de conclusão`;
        if (barEl) barEl.style.width = `${rate}%`;
        if (textEl) textEl.innerText = `${summary.completedCount} tarefas concluídas de ${summary.totalRecords} agendamentos registrados.`;

        // 2. Best Times List
        const bestListEl = document.getElementById('habits-best-times-list');
        if (bestListEl) {
            if (summary.bestTimes.length === 0) {
                bestListEl.innerHTML = `<div class="card" style="padding: 12px; font-size: 13px; color: var(--text-secondary);">Ainda observando horários para identificar os momentos de maior sucesso.</div>`;
            } else {
                bestListEl.innerHTML = summary.bestTimes.map(b => `
                    <div class="card habit-card" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                        <div>
                            <h4 style="font-size: 14px; font-weight: 700;">${escapeHtml(b.title)}</h4>
                            <p style="font-size: 12px; color: var(--text-secondary); margin-top: 2px;">Melhor momento: <strong>${b.slotName}</strong></p>
                            <span style="font-size: 11px; color: var(--text-secondary); opacity: 0.8;">${b.reason}</span>
                        </div>
                        <span class="habit-badge-rate">${Math.round(b.rate * 100)}%</span>
                    </div>
                `).join('');
            }
        }

        // 3. Duration Insights
        const durListEl = document.getElementById('habits-duration-insights-list');
        if (durListEl) {
            if (summary.durationInsights.length === 0) {
                durListEl.innerHTML = `<div class="card" style="padding: 12px; font-size: 13px; color: var(--text-secondary);">Suas durações reais estão equilibradas com os valores planejados.</div>`;
            } else {
                durListEl.innerHTML = summary.durationInsights.map(d => `
                    <div class="card habit-card" style="margin-bottom: 8px;">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                            <h4 style="font-size: 14px; font-weight: 700;">${escapeHtml(d.title)}</h4>
                            <span class="badge" style="background-color: rgba(59, 130, 246, 0.15); color: var(--secondary-color); font-weight: 700;">Planejado: ${d.plannedDuration}m → Real: ${d.learnedMedian}m</span>
                        </div>
                        <p style="font-size: 12px; color: var(--text-secondary); margin-bottom: 10px;">${d.suggestion}</p>
                        <button class="btn-primary btn-apply-duration" data-id="${d.activityId}" data-dur="${d.learnedMedian}" style="padding: 8px 12px; font-size: 12px; width: 100%;">
                            Atualizar duração sugerida para ${d.learnedMedian} min
                        </button>
                    </div>
                `).join('');

                durListEl.querySelectorAll('.btn-apply-duration').forEach(btn => {
                    btn.addEventListener('click', () => {
                        const actId = parseInt(btn.getAttribute('data-id'), 10);
                        const newDur = parseInt(btn.getAttribute('data-dur'), 10);
                        const actList = Storage.getActivities();
                        const target = actList.find(a => a.id === actId);
                        if (target) {
                            target.estimatedDurationMinutes = newDur;
                            Storage.saveActivities(actList);
                            alert(`Duração de "${target.title}" atualizada para ${newDur} minutos!`);
                            refreshHabits();
                            refreshActivities();
                        }
                    });
                });
            }
        }

        // 4. Frequently Postponed
        const postListEl = document.getElementById('habits-postponed-list');
        if (postListEl) {
            if (summary.postponed.length === 0) {
                postListEl.innerHTML = `<div class="card" style="padding: 12px; font-size: 13px; color: var(--text-secondary);">Nenhum padrão frequente de adiamento detectado recentemente.</div>`;
            } else {
                postListEl.innerHTML = summary.postponed.map(p => `
                    <div class="card habit-card" style="border-left: 3px solid #f59e0b; margin-bottom: 8px;">
                        <h4 style="font-size: 14px; font-weight: 700; color: #f59e0b;">${escapeHtml(p.title)}</h4>
                        <p style="font-size: 12px; color: var(--text-secondary); margin-top: 4px;">${p.suggestion}</p>
                    </div>
                `).join('');
            }
        }

        // 5. Category Distribution
        const catBoxEl = document.getElementById('habits-category-distribution-box');
        if (catBoxEl) {
            const categories = Object.keys(summary.categoryMinutes);
            const totalMin = categories.reduce((acc, c) => acc + summary.categoryMinutes[c], 0);

            if (categories.length === 0 || totalMin === 0) {
                catBoxEl.innerHTML = `<p style="font-size: 13px; color: var(--text-secondary);">Sem histórico de tempo para distribuir.</p>`;
            } else {
                const colors = {
                    'Trabalho': '#8b5cf6',
                    'Estudos': '#3b82f6',
                    'Saúde': '#10b981',
                    'Casa': '#f59e0b',
                    'Lazer': '#ec4899',
                    'Geral': '#6366f1'
                };
                catBoxEl.innerHTML = categories.map(cat => {
                    const min = summary.categoryMinutes[cat];
                    const pct = Math.round((min / totalMin) * 100);
                    const color = colors[cat] || '#6366f1';
                    return `
                        <div class="category-bar-row">
                            <div style="display: flex; justify-content: space-between; font-size: 12px;">
                                <span style="font-weight: 600;">${cat}</span>
                                <span style="color: var(--text-secondary);">${formatMinutesToHours(min)} (${pct}%)</span>
                            </div>
                            <div class="category-bar-track">
                                <div class="category-bar-fill" style="width: ${pct}%; background-color: ${color};"></div>
                            </div>
                        </div>
                    `;
                }).join('');
            }
        }
    }

    // --- Initialization & Event Listeners ---
    function init() {
        applyTheme();
        Navigation.init();
        initOnboarding();
        setupEventListeners();
    }

    function setupEventListeners() {
        // Onboarding
        const btnStart = document.getElementById('onboarding-btn-start');
        if (btnStart) btnStart.addEventListener('click', nextOnboardingStep);

        const btnBack = document.getElementById('onboarding-btn-back');
        if (btnBack) btnBack.addEventListener('click', prevOnboardingStep);

        const nameInput = document.getElementById('onboarding-input-name');
        if (nameInput) {
            nameInput.addEventListener('input', (e) => {
                Storage.updateField('name', e.target.value);
                const btnNextName = document.getElementById('onboarding-btn-name-next');
                if (btnNextName) {
                    btnNextName.innerText = e.target.value.trim().length === 0 ? 'Continuar sem informar' : 'Avançar';
                }
            });
        }
        const btnNextName = document.getElementById('onboarding-btn-name-next');
        if (btnNextName) btnNextName.addEventListener('click', nextOnboardingStep);

        const wakeBtn = document.getElementById('onboarding-wake-btn');
        if (wakeBtn) {
            wakeBtn.addEventListener('click', () => {
                const profile = Storage.getProfile();
                openTimePicker(profile.wakeUpTime, 'Horário de acordar', (selectedTime) => {
                    Storage.updateField('wakeUpTime', selectedTime);
                    updateOnboardingTimesDisplay();
                });
            });
        }

        const sleepBtn = document.getElementById('onboarding-sleep-btn');
        if (sleepBtn) {
            sleepBtn.addEventListener('click', () => {
                const profile = Storage.getProfile();
                openTimePicker(profile.sleepTime, 'Horário de dormir', (selectedTime) => {
                    Storage.updateField('sleepTime', selectedTime);
                    updateOnboardingTimesDisplay();
                });
            });
        }
        const btnNextTimes = document.getElementById('onboarding-btn-times-next');
        if (btnNextTimes) btnNextTimes.addEventListener('click', nextOnboardingStep);

        document.querySelectorAll('.onboarding-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const val = chip.getAttribute('data-value');
                if (val) toggleOnboardingCommitment(val);
            });
        });
        const btnNextCommitments = document.getElementById('onboarding-btn-commitments-next');
        if (btnNextCommitments) btnNextCommitments.addEventListener('click', nextOnboardingStep);

        document.querySelectorAll('.org-mode-card').forEach(card => {
            card.addEventListener('click', () => {
                const mode = card.getAttribute('data-mode');
                if (mode) setOrganizationMode(mode);
            });
        });
        const btnNextOrg = document.getElementById('onboarding-btn-org-next');
        if (btnNextOrg) btnNextOrg.addEventListener('click', nextOnboardingStep);

        const btnFinish = document.getElementById('onboarding-btn-finish');
        if (btnFinish) btnFinish.addEventListener('click', finishOnboarding);

        // Home Actions
        const quickActions = document.querySelectorAll('.quick-action-btn');
        quickActions.forEach(btn => {
            btn.addEventListener('click', () => {
                const target = btn.getAttribute('data-nav');
                const action = btn.getAttribute('data-action');
                if (action === 'add-flexible') openAddActivitySheet('FLEXIBLE');
                else if (action === 'add-fixed') openAddActivitySheet('FIXED');
                else if (target) Navigation.goToTab(target);
            });
        });

        const homeEmptyAction = document.getElementById('home-empty-action');
        if (homeEmptyAction) homeEmptyAction.addEventListener('click', () => openAddActivitySheet('FLEXIBLE'));

        const homeFab = document.getElementById('home-fab');
        if (homeFab) homeFab.addEventListener('click', () => openAddActivitySheet('FLEXIBLE'));

        // Planning Navigation & FAB
        const weekPrevBtn = document.getElementById('week-nav-prev');
        if (weekPrevBtn) weekPrevBtn.addEventListener('click', () => {
            selectedPlanningDate.setDate(selectedPlanningDate.getDate() - 7);
            refreshPlanning();
        });

        const weekNextBtn = document.getElementById('week-nav-next');
        if (weekNextBtn) weekNextBtn.addEventListener('click', () => {
            selectedPlanningDate.setDate(selectedPlanningDate.getDate() + 7);
            refreshPlanning();
        });

        const weekTodayBtn = document.getElementById('week-nav-today-btn');
        if (weekTodayBtn) weekTodayBtn.addEventListener('click', () => {
            selectedPlanningDate = new Date();
            refreshPlanning();
        });

        const planAddFab = document.getElementById('planning-btn-add');
        if (planAddFab) planAddFab.addEventListener('click', () => openScheduleEntrySheet(9, 0, 60));

        const planEmptyAdd = document.getElementById('planning-empty-btn');
        if (planEmptyAdd) planEmptyAdd.addEventListener('click', () => openScheduleEntrySheet(9, 0, 60));

        const planMyActs = document.getElementById('plan-btn-my-activities');
        if (planMyActs) {
            planMyActs.addEventListener('click', () => {
                Navigation.goToTab('activities');
            });
        }

        // Schedule Entry Sheet Listeners (Etapa 3)
        const schedCloseBtn = document.getElementById('sched-sheet-btn-close');
        if (schedCloseBtn) schedCloseBtn.addEventListener('click', closeScheduleEntrySheet);

        const schedTimePickerBtn = document.getElementById('sched-time-picker-btn');
        if (schedTimePickerBtn) {
            schedTimePickerBtn.addEventListener('click', () => {
                openTimePicker(formatTime(schedStartHour, schedStartMinute), 'Horário de início', (t, h, m) => {
                    schedStartHour = h;
                    schedStartMinute = m;
                    updateSchedTimeUI();
                });
            });
        }

        document.querySelectorAll('.sched-dur-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                schedDurationMinutes = parseInt(chip.getAttribute('data-mins'), 10);
                updateSchedDurationUI();
            });
        });

        const schedConfirmBtn = document.getElementById('sched-btn-confirm');
        if (schedConfirmBtn) schedConfirmBtn.addEventListener('click', confirmScheduleEntry);

        // Completion Prompt Modal Listeners (Etapa 3 & 4)
        const btnCompleteAdvance = document.getElementById('btn-complete-advance');
        if (btnCompleteAdvance) {
            btnCompleteAdvance.addEventListener('click', () => {
                if (activePromptEntryId) {
                    const all = Storage.getAllScheduleEntries();
                    const entry = all.find(s => s.id === activePromptEntryId);
                    if (entry && entry.activityId) {
                        Storage.advanceContext(entry.activityId);
                    }
                    Storage.updateScheduleStatus(activePromptEntryId, 'COMPLETED');
                    closeCompletionPromptModal();
                    refreshPlanning();
                    refreshHome();
                }
            });
        }

        const btnCompleteYes = document.getElementById('btn-complete-yes');
        if (btnCompleteYes) {
            btnCompleteYes.addEventListener('click', () => {
                if (activePromptEntryId) {
                    Storage.updateScheduleStatus(activePromptEntryId, 'COMPLETED');
                    closeCompletionPromptModal();
                    refreshPlanning();
                    refreshHome();
                }
            });
        }

        const btnCompleteStill = document.getElementById('btn-complete-still');
        if (btnCompleteStill) {
            btnCompleteStill.addEventListener('click', () => {
                if (activePromptEntryId) {
                    const id = activePromptEntryId;
                    closeCompletionPromptModal();
                    openTimeExtensionModal(id);
                }
            });
        }

        const btnCompletePostpone = document.getElementById('btn-complete-postpone');
        if (btnCompletePostpone) {
            btnCompletePostpone.addEventListener('click', openPostponeModal);
        }

        const btnCompleteNotDone = document.getElementById('btn-complete-not-done');
        if (btnCompleteNotDone) {
            btnCompleteNotDone.addEventListener('click', () => {
                if (activePromptEntryId) {
                    Storage.updateScheduleStatus(activePromptEntryId, 'NOT_DONE');
                    closeCompletionPromptModal();
                    refreshPlanning();
                    refreshHome();
                }
            });
        }

        // Postpone Modal Listeners (Etapa 3)
        const btnPostponeTomorrow = document.getElementById('btn-postpone-tomorrow');
        if (btnPostponeTomorrow) {
            btnPostponeTomorrow.addEventListener('click', () => {
                if (activePromptEntryId) {
                    const tomorrow = new Date(selectedPlanningDate);
                    tomorrow.setDate(tomorrow.getDate() + 1);
                    Storage.postponeScheduleEntry(activePromptEntryId, toISODate(tomorrow));
                    closePostponeModal();
                    refreshPlanning();
                    refreshHome();
                }
            });
        }

        const btnPostponeKeep = document.getElementById('btn-postpone-keep');
        if (btnPostponeKeep) btnPostponeKeep.addEventListener('click', closePostponeModal);

        const btnPostponeCancel = document.getElementById('btn-postpone-cancel-entry');
        if (btnPostponeCancel) {
            btnPostponeCancel.addEventListener('click', () => {
                if (activePromptEntryId) {
                    Storage.deleteScheduleEntry(activePromptEntryId);
                    closePostponeModal();
                    refreshPlanning();
                    refreshHome();
                }
            });
        }

        // Activities Management Tab Listeners
        const activitiesBackBtn = document.getElementById('activities-btn-back');
        if (activitiesBackBtn) {
            activitiesBackBtn.addEventListener('click', () => {
                Navigation.goToTab('planning');
            });
        }

        const activitiesFab = document.getElementById('activities-fab');
        if (activitiesFab) activitiesFab.addEventListener('click', () => openAddActivitySheet('FLEXIBLE'));

        const activitiesEmptyBtn = document.getElementById('activities-empty-btn');
        if (activitiesEmptyBtn) activitiesEmptyBtn.addEventListener('click', () => openAddActivitySheet('FLEXIBLE'));

        const searchInput = document.getElementById('activities-search-input');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                activitySearchQuery = e.target.value;
                refreshActivities();
            });
        }

        // Type filter chips
        document.querySelectorAll('.type-filter-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const type = chip.getAttribute('data-type');
                activityTypeFilter = type === 'ALL' ? null : type;
                document.querySelectorAll('.type-filter-chip').forEach(c => c.classList.remove('selected'));
                chip.classList.add('selected');
                refreshActivities();
            });
        });

        // Category filter chips
        document.querySelectorAll('.cat-filter-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const cat = chip.getAttribute('data-cat');
                activityCategoryFilter = cat === 'ALL' ? null : cat;
                document.querySelectorAll('.cat-filter-chip').forEach(c => c.classList.remove('selected'));
                chip.classList.add('selected');
                refreshActivities();
            });
        });

        // Add Activity Sheet Controls
        const sheetCloseBtn = document.getElementById('sheet-btn-close');
        if (sheetCloseBtn) sheetCloseBtn.addEventListener('click', closeAddActivitySheet);

        const sheetSaveBtn = document.getElementById('sheet-btn-save');
        if (sheetSaveBtn) sheetSaveBtn.addEventListener('click', saveSheetActivity);

        document.querySelectorAll('.sheet-type-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                sheetType = chip.getAttribute('data-type');
                updateSheetTypeUI();
            });
        });

        document.querySelectorAll('.sheet-category-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                sheetCategory = chip.getAttribute('data-category');
                updateSheetCategoryUI();
            });
        });

        const sheetTitleInput = document.getElementById('sheet-input-title');
        if (sheetTitleInput) {
            sheetTitleInput.addEventListener('input', updateSheetSuggestions);
        }

        document.querySelectorAll('.sheet-dur-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const mins = parseInt(chip.getAttribute('data-mins'), 10);
                sheetDurationMinutes = mins;
                updateSheetDurationUI();
            });
        });

        const sheetCustomDurBtn = document.getElementById('sheet-dur-custom-btn');
        if (sheetCustomDurBtn) {
            sheetCustomDurBtn.addEventListener('click', () => {
                openTimePicker(formatTime(Math.floor(sheetDurationMinutes / 60), sheetDurationMinutes % 60), 'Duração personalizada', (t, h, m) => {
                    const total = h * 60 + m;
                    sheetDurationMinutes = total > 0 ? total : 15;
                    updateSheetDurationUI();
                });
            });
        }

        const sheetCountMinus = document.getElementById('sheet-count-minus');
        const sheetCountPlus = document.getElementById('sheet-count-plus');
        if (sheetCountMinus) {
            sheetCountMinus.addEventListener('click', () => {
                if (sheetCount > 1) {
                    sheetCount--;
                    updateSheetCountUI();
                }
            });
        }
        if (sheetCountPlus) {
            sheetCountPlus.addEventListener('click', () => {
                if (sheetCount < 10) {
                    sheetCount++;
                    updateSheetCountUI();
                }
            });
        }

        const sheetStartBtn = document.getElementById('sheet-start-btn');
        if (sheetStartBtn) {
            sheetStartBtn.addEventListener('click', () => {
                openTimePicker(formatTime(sheetStartHour, sheetStartMinute), 'Horário de início', (t, h, m) => {
                    sheetStartHour = h;
                    sheetStartMinute = m;
                    updateSheetFixedTimeUI();
                });
            });
        }

        const sheetEndBtn = document.getElementById('sheet-end-btn');
        if (sheetEndBtn) {
            sheetEndBtn.addEventListener('click', () => {
                openTimePicker(formatTime(sheetEndHour, sheetEndMinute), 'Horário de término', (t, h, m) => {
                    sheetEndHour = h;
                    sheetEndMinute = m;
                    updateSheetFixedTimeUI();
                });
            });
        }

        document.querySelectorAll('.sheet-rec-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                sheetRecurrence = chip.getAttribute('data-rec');
                updateSheetFixedTimeUI();
            });
        });

        const sheetWeeklyMinus = document.getElementById('sheet-weekly-minus');
        const sheetWeeklyPlus = document.getElementById('sheet-weekly-plus');
        if (sheetWeeklyMinus) {
            sheetWeeklyMinus.addEventListener('click', () => {
                if (sheetWeeklyTarget > 1) {
                    sheetWeeklyTarget--;
                    updateSheetWeeklyTargetUI();
                }
            });
        }
        if (sheetWeeklyPlus) {
            sheetWeeklyPlus.addEventListener('click', () => {
                if (sheetWeeklyTarget < 7) {
                    sheetWeeklyTarget++;
                    updateSheetWeeklyTargetUI();
                }
            });
        }

        // Delete Confirm Modal Buttons
        const deleteCancelBtn = document.getElementById('modal-delete-cancel');
        const deleteConfirmBtn = document.getElementById('modal-delete-confirm');
        if (deleteCancelBtn) deleteCancelBtn.addEventListener('click', closeDeleteModal);
        if (deleteConfirmBtn) deleteConfirmBtn.addEventListener('click', confirmDeleteActivity);

        // Time Picker Modal Buttons
        const pickerHourPlus = document.getElementById('tp-hour-plus');
        const pickerHourMinus = document.getElementById('tp-hour-minus');
        const pickerMinPlus = document.getElementById('tp-min-plus');
        const pickerMinMinus = document.getElementById('tp-min-minus');
        const pickerCancel = document.getElementById('tp-btn-cancel');
        const pickerConfirm = document.getElementById('tp-btn-confirm');

        if (pickerHourPlus) pickerHourPlus.addEventListener('click', incrementHour);
        if (pickerHourMinus) pickerHourMinus.addEventListener('click', decrementHour);
        if (pickerMinPlus) pickerMinPlus.addEventListener('click', incrementMinute);
        if (pickerMinMinus) pickerMinMinus.addEventListener('click', decrementMinute);
        if (pickerCancel) pickerCancel.addEventListener('click', closeTimePicker);
        if (pickerConfirm) pickerConfirm.addEventListener('click', confirmTimePicker);

        // Settings Screen Inputs
        const settingsNameInput = document.getElementById('settings-input-name');
        if (settingsNameInput) {
            settingsNameInput.addEventListener('input', (e) => {
                Storage.updateField('name', e.target.value);
            });
        }

        const settingsWakeBtn = document.getElementById('settings-wake-btn');
        if (settingsWakeBtn) {
            settingsWakeBtn.addEventListener('click', () => {
                const profile = Storage.getProfile();
                openTimePicker(profile.wakeUpTime, 'Horário de acordar', (selectedTime) => {
                    Storage.updateField('wakeUpTime', selectedTime);
                    refreshSettings();
                });
            });
        }

        const settingsSleepBtn = document.getElementById('settings-sleep-btn');
        if (settingsSleepBtn) {
            settingsSleepBtn.addEventListener('click', () => {
                const profile = Storage.getProfile();
                openTimePicker(profile.sleepTime, 'Horário de dormir', (selectedTime) => {
                    Storage.updateField('sleepTime', selectedTime);
                    refreshSettings();
                });
            });
        }

        document.querySelectorAll('input[name="theme-option"]').forEach(radio => {
            radio.addEventListener('change', () => {
                if (radio.checked) setTheme(radio.value);
            });
        });

        document.querySelectorAll('input[name="org-option"]').forEach(radio => {
            radio.addEventListener('change', () => {
                if (radio.checked) Storage.updateField('organizationMode', radio.value);
            });
        });

        document.querySelectorAll('input[name="balance-option"]').forEach(radio => {
            radio.addEventListener('change', () => {
                if (radio.checked) Storage.updateField('balancePreference', radio.value);
            });
        });

        // Recommendation card listeners (Etapa 5)
        const recDismissBtn = document.getElementById('rec-btn-dismiss');
        const recLaterBtn = document.getElementById('rec-btn-later');
        const recNotInterestedBtn = document.getElementById('rec-btn-not-interested');
        const recAcceptBtn = document.getElementById('rec-btn-accept');

        if (recDismissBtn) recDismissBtn.addEventListener('click', () => {
            if (currentRecommendation) {
                Storage.recordRecommendationFeedback({
                    recommendationType: currentRecommendation.type,
                    activityId: currentRecommendation.suggestedActivity ? currentRecommendation.suggestedActivity.id : null,
                    activityTitle: currentRecommendation.suggestedActivity ? currentRecommendation.suggestedActivity.title : '',
                    action: 'DISMISSED'
                });
            }
            isRecommendationDismissed = true;
            const card = document.getElementById('home-recommendation-card');
            if (card) card.classList.add('hidden');
        });

        if (recLaterBtn) recLaterBtn.addEventListener('click', () => {
            if (currentRecommendation) {
                Storage.recordRecommendationFeedback({
                    recommendationType: currentRecommendation.type,
                    activityId: currentRecommendation.suggestedActivity ? currentRecommendation.suggestedActivity.id : null,
                    activityTitle: currentRecommendation.suggestedActivity ? currentRecommendation.suggestedActivity.title : '',
                    action: 'DISMISSED'
                });
            }
            isRecommendationDismissed = true;
            const card = document.getElementById('home-recommendation-card');
            if (card) card.classList.add('hidden');
        });

        if (recNotInterestedBtn) recNotInterestedBtn.addEventListener('click', () => {
            if (currentRecommendation) {
                Storage.recordRecommendationFeedback({
                    recommendationType: currentRecommendation.type,
                    activityId: currentRecommendation.suggestedActivity ? currentRecommendation.suggestedActivity.id : null,
                    activityTitle: currentRecommendation.suggestedActivity ? currentRecommendation.suggestedActivity.title : '',
                    action: 'NOT_INTERESTED'
                });
            }
            isRecommendationDismissed = true;
            const card = document.getElementById('home-recommendation-card');
            if (card) card.classList.add('hidden');
            alert('Anotado! Reduziremos essa sugestão nos seus próximos dias.');
        });

        if (recAcceptBtn) recAcceptBtn.addEventListener('click', () => {
            if (currentRecommendation) {
                const act = currentRecommendation.suggestedActivity;
                if (currentRecommendation.type === 'LEARNED_DURATION' && act) {
                    act.estimatedDurationMinutes = currentRecommendation.suggestedDurationMinutes;
                    Storage.saveActivity(act);
                    Storage.recordRecommendationFeedback({
                        recommendationType: currentRecommendation.type,
                        activityId: act.id,
                        activityTitle: act.title,
                        action: 'ACCEPTED'
                    });
                    alert(`Duração de "${act.title}" atualizada para ${currentRecommendation.suggestedDurationMinutes} min!`);
                    isRecommendationDismissed = true;
                    refreshActivities();
                    refreshHabits();
                    refreshHome();
                } else if (act) {
                    const now = new Date();
                    const startH = currentRecommendation.suggestedStartHour !== undefined ? currentRecommendation.suggestedStartHour : now.getHours();
                    const startM = currentRecommendation.suggestedStartMinute !== undefined ? currentRecommendation.suggestedStartMinute : now.getMinutes();
                    Storage.saveScheduleEntry({
                        activityId: act.id,
                        activityTitle: act.title,
                        category: act.category,
                        date: toISODate(now),
                        startHour: startH,
                        startMinute: startM,
                        targetDurationMinutes: currentRecommendation.suggestedDurationMinutes,
                        status: 'PENDING'
                    });
                    Storage.recordRecommendationFeedback({
                        recommendationType: currentRecommendation.type,
                        activityId: act.id,
                        activityTitle: act.title,
                        action: 'ACCEPTED'
                    });
                    isRecommendationDismissed = true;
                    refreshHome();
                    refreshPlanning();
                }
            }
        });

        // Habits Demo Controls (Section 17)
        const btnLoadDemo = document.getElementById('habits-btn-load-demo');
        if (btnLoadDemo) {
            btnLoadDemo.addEventListener('click', () => {
                Storage.seedDemonstrationHistory();
                alert('Cenário de demonstração carregado com sucesso!\nOs dados de histórico agora exibem padrões estatísticos e melhores horários.');
                refreshHabits();
                refreshHome();
            });
        }

        const btnClearDemo = document.getElementById('habits-btn-clear-demo');
        if (btnClearDemo) {
            btnClearDemo.addEventListener('click', () => {
                Storage.clearLearningData();
                alert('Histórico de execuções limpo.');
                refreshHabits();
                refreshHome();
            });
        }

        // Settings Learning Controls (Section 14)
        const toggleRec = document.getElementById('setting-toggle-recommendations');
        if (toggleRec) {
            toggleRec.addEventListener('change', (e) => {
                const s = Storage.getLearningSettings();
                s.personalizedRecommendationsEnabled = e.target.checked;
                Storage.saveLearningSettings(s);
                refreshHome();
            });
        }

        const toggleLearn = document.getElementById('setting-toggle-learning');
        if (toggleLearn) {
            toggleLearn.addEventListener('change', (e) => {
                const s = Storage.getLearningSettings();
                s.learningEnabled = e.target.checked;
                Storage.saveLearningSettings(s);
                refreshHome();
                refreshHabits();
            });
        }

        const toggleExp = document.getElementById('setting-toggle-explanations');
        if (toggleExp) {
            toggleExp.addEventListener('change', (e) => {
                const s = Storage.getLearningSettings();
                s.showExplanationsEnabled = e.target.checked;
                Storage.saveLearningSettings(s);
                refreshHome();
            });
        }

        const btnRestoreDur = document.getElementById('settings-btn-restore-durations');
        if (btnRestoreDur) {
            btnRestoreDur.addEventListener('click', () => {
                if (confirm('Restaurar as durações padrão sugeridas para todas as atividades?')) {
                    const activities = Storage.getActivities();
                    activities.forEach(a => {
                        if (a.title.toLowerCase().includes('louça') || a.title.toLowerCase().includes('cozinha')) a.estimatedDurationMinutes = 30;
                        if (a.title.toLowerCase().includes('estudo') || a.title.toLowerCase().includes('programação')) a.estimatedDurationMinutes = 90;
                        if (a.title.toLowerCase().includes('treino') || a.title.toLowerCase().includes('academia')) a.estimatedDurationMinutes = 60;
                    });
                    Storage.saveActivities(activities);
                    alert('Durações restauradas para os valores padrão.');
                    refreshActivities();
                    refreshHabits();
                    refreshHome();
                }
            });
        }

        const btnClearLearn = document.getElementById('settings-btn-clear-learning');
        if (btnClearLearn) {
            btnClearLearn.addEventListener('click', () => {
                if (confirm('Limpar histórico de execuções analisado e feedback das sugestões?\n\nSuas atividades e compromissos agendados continuarão salvos.')) {
                    Storage.clearLearningData();
                    alert('Dados de aprendizado redefinidos.');
                    refreshHabits();
                    refreshHome();
                }
            });
        }

        // Auto-Organization Buttons (Etapa 4)
        const btnSuggestRoutine = document.getElementById('btn-suggest-routine');
        if (btnSuggestRoutine) {
            btnSuggestRoutine.addEventListener('click', () => {
                const profile = Storage.getProfile();
                const activities = Storage.getActivities();
                const scheduled = Storage.getScheduleEntries(toISODate(selectedPlanningDate));
                const contexts = Storage.getContexts();
                const contextMap = {};
                contexts.forEach(c => contextMap[c.activityId] = c);

                const proposal = AutoOrganizationEngine.generateProposal(
                    selectedPlanningDate,
                    profile.wakeUpTime,
                    profile.sleepTime,
                    activities.filter(a => a.type === 'FIXED'),
                    scheduled,
                    activities.filter(a => a.type === 'FLEXIBLE'),
                    contextMap,
                    profile.balancePreference || 'BALANCED'
                );
                openProposalModal(proposal);
            });
        }

        const btnReorganizeDay = document.getElementById('btn-reorganize-day');
        if (btnReorganizeDay) {
            btnReorganizeDay.addEventListener('click', () => {
                const profile = Storage.getProfile();
                const activities = Storage.getActivities();
                const scheduled = Storage.getScheduleEntries(toISODate(selectedPlanningDate));
                const contexts = Storage.getContexts();
                const contextMap = {};
                contexts.forEach(c => contextMap[c.activityId] = c);

                const proposal = AutoOrganizationEngine.reorganizeRemainingDay(
                    new Date(),
                    selectedPlanningDate,
                    profile.wakeUpTime,
                    profile.sleepTime,
                    activities.filter(a => a.type === 'FIXED'),
                    scheduled,
                    activities.filter(a => a.type === 'FLEXIBLE'),
                    contextMap,
                    profile.balancePreference || 'BALANCED'
                );
                openProposalModal(proposal);
            });
        }

        // Proposal Modal Actions (Etapa 4)
        const btnProposalClose = document.getElementById('proposal-btn-close');
        if (btnProposalClose) btnProposalClose.addEventListener('click', closeProposalModal);

        const btnProposalDiscard = document.getElementById('btn-proposal-discard');
        if (btnProposalDiscard) btnProposalDiscard.addEventListener('click', closeProposalModal);

        const btnProposalApprove = document.getElementById('btn-proposal-approve');
        if (btnProposalApprove) {
            btnProposalApprove.addEventListener('click', () => {
                if (currentRoutineProposal && currentRoutineProposal.items.length > 0) {
                    const activities = Storage.getActivities();
                    const dateStr = currentRoutineProposal.date;

                    currentRoutineProposal.items.forEach(item => {
                        const act = activities.find(a => a.id === item.activityId);
                        if (act) {
                            const parsed = parseTime(item.startTime);
                            Storage.saveScheduleEntry({
                                activityId: act.id,
                                activityTitle: act.title,
                                category: act.category,
                                date: dateStr,
                                startHour: parsed.hour,
                                startMinute: parsed.minute,
                                targetDurationMinutes: item.durationMinutes,
                                status: 'PENDING'
                            });
                        }
                    });

                    closeProposalModal();
                    refreshPlanning();
                    refreshHome();
                }
            });
        }

        const resetBtn = document.getElementById('settings-btn-reset');
        if (resetBtn) {
            resetBtn.addEventListener('click', () => {
                if (confirm('Deseja reiniciar a configuração inicial do LifeFlow e limpar os dados de teste?')) {
                    Storage.resetToDefault();
                    applyTheme();
                    initOnboarding();
                    Navigation.showOnboardingScreen();
                    showOnboardingStep(1);
                }
            });
        }

        // ==========================================
        // ETAPA 6: FOCUS & TRACKING & EXTENSION LISTENERS
        // ==========================================

        // Focus Banner Controls
        const btnFocusPause = document.getElementById('focus-banner-btn-pause');
        if (btnFocusPause) {
            btnFocusPause.addEventListener('click', () => {
                const session = Storage.getActiveFocusSession();
                if (!session) return;
                if (session.isPaused) Storage.resumeFocusSession();
                else Storage.pauseFocusSession();
                updateFocusSessionUI();
            });
        }

        const btnFocusExtend = document.getElementById('focus-banner-btn-extend');
        if (btnFocusExtend) {
            btnFocusExtend.addEventListener('click', () => {
                Storage.extendFocusSession(15);
                updateFocusSessionUI();
            });
        }

        const btnFocusDone = document.getElementById('focus-banner-btn-done');
        if (btnFocusDone) {
            btnFocusDone.addEventListener('click', () => {
                Storage.concludeFocusSession('Momento de foco concluído!');
                updateFocusSessionUI();
                refreshHome();
                refreshPlanning();
            });
        }

        // Focus Modal Controls
        const btnModalFocusPause = document.getElementById('focus-modal-btn-pause');
        if (btnModalFocusPause) {
            btnModalFocusPause.addEventListener('click', () => {
                const session = Storage.getActiveFocusSession();
                if (!session) return;
                if (session.isPaused) Storage.resumeFocusSession();
                else Storage.pauseFocusSession();
                updateFocusSessionUI();
            });
        }

        const btnModalFocusExtend = document.getElementById('focus-modal-btn-extend');
        if (btnModalFocusExtend) {
            btnModalFocusExtend.addEventListener('click', () => {
                Storage.extendFocusSession(15);
                updateFocusSessionUI();
            });
        }

        const btnModalFocusDone = document.getElementById('focus-modal-btn-done');
        if (btnModalFocusDone) {
            btnModalFocusDone.addEventListener('click', () => {
                Storage.concludeFocusSession('Momento de foco concluído!');
                closeFocusModal();
                updateFocusSessionUI();
                refreshHome();
                refreshPlanning();
            });
        }

        const btnModalFocusClose = document.getElementById('focus-modal-btn-close');
        if (btnModalFocusClose) {
            btnModalFocusClose.addEventListener('click', closeFocusModal);
        }

        // Day Tracking Quick Action Buttons
        const trackingBtnComplete = document.getElementById('tracking-btn-complete');
        if (trackingBtnComplete) {
            trackingBtnComplete.addEventListener('click', () => {
                if (currentTrackingEntry) {
                    openCompletionPromptModal(currentTrackingEntry.id);
                }
            });
        }

        const trackingBtnExtend = document.getElementById('tracking-btn-extend');
        if (trackingBtnExtend) {
            trackingBtnExtend.addEventListener('click', () => {
                if (currentTrackingEntry) {
                    openTimeExtensionModal(currentTrackingEntry.id);
                }
            });
        }

        const trackingBtnFocus = document.getElementById('tracking-btn-focus');
        if (trackingBtnFocus) {
            trackingBtnFocus.addEventListener('click', () => {
                if (currentTrackingEntry) {
                    Storage.startFocusSession({
                        activityId: currentTrackingEntry.activityId,
                        activityTitle: currentTrackingEntry.activityTitle,
                        category: currentTrackingEntry.category,
                        plannedDurationMinutes: currentTrackingEntry.targetDurationMinutes,
                        scheduleEntryId: currentTrackingEntry.id
                    });
                    Storage.updateScheduleStatus(currentTrackingEntry.id, 'IN_PROGRESS');
                    openFocusModal();
                    updateFocusSessionUI();
                    refreshHome();
                }
            });
        }

        // Time Extension Modal Listeners
        document.querySelectorAll('.ext-quick-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.ext-quick-btn').forEach(b => {
                    b.style.borderColor = 'var(--outline-color)';
                    b.style.background = 'transparent';
                });
                btn.style.borderColor = 'var(--primary-color)';
                btn.style.background = 'var(--primary-container)';
                selectedExtensionMinutes = parseInt(btn.getAttribute('data-min'), 10) || 15;
                checkExtensionConflict();
            });
        });

        const btnExtCancel = document.getElementById('ext-btn-cancel');
        if (btnExtCancel) {
            btnExtCancel.addEventListener('click', closeTimeExtensionModal);
        }

        const btnExtConfirm = document.getElementById('ext-btn-confirm');
        if (btnExtConfirm) {
            btnExtConfirm.addEventListener('click', () => {
                if (activeExtensionEntryId) {
                    const allEntries = Storage.getAllScheduleEntries();
                    const entry = allEntries.find(e => e.id === activeExtensionEntryId);
                    if (entry) {
                        const newDur = (entry.targetDurationMinutes || 30) + selectedExtensionMinutes;
                        const startM = entry.startHour * 60 + entry.startMinute;
                        const endM = startM + newDur;
                        const updated = {
                            ...entry,
                            targetDurationMinutes: newDur,
                            endHour: Math.floor(endM / 60) % 24,
                            endMinute: endM % 60,
                            status: 'IN_PROGRESS'
                        };
                        Storage.updateScheduleEntry(updated);
                    }
                    closeTimeExtensionModal();
                    refreshHome();
                    refreshPlanning();
                }
            });
        }

        // Notification and Usage Settings Toggles
        const bindNotifSetting = (id, prop) => {
            const el = document.getElementById(id);
            if (el) {
                el.addEventListener('change', (e) => {
                    const current = Storage.getNotificationSettings();
                    current[prop] = e.target.checked;
                    Storage.saveNotificationSettings(current);
                });
            }
        };

        bindNotifSetting('setting-toggle-reminders', 'remindersEnabled');
        bindNotifSetting('setting-toggle-notify-before', 'notifyBeforeStart');
        bindNotifSetting('setting-toggle-completion-prompt', 'promptCompletionOnEnd');
        bindNotifSetting('setting-toggle-quiet-hours', 'quietHoursEnabled');
        bindNotifSetting('setting-toggle-usage-consent', 'usageMonitoringConsentGranted');

        // Continuous focus timer ticker
        if (typeof window !== 'undefined') {
            setInterval(updateFocusTimerTick, 1000);
        }
    }

    // ==========================================
    // ETAPA 6: HELPER FUNCTIONS (FOCUS, TRACKING, BALANCE)
    // ==========================================

    let currentTrackingEntry = null;
    let activeExtensionEntryId = null;
    let selectedExtensionMinutes = 15;

    function openFocusModal() {
        const modal = document.getElementById('modal-focus-session');
        const session = Storage.getActiveFocusSession();
        if (modal) {
            if (session) {
                const title = document.getElementById('focus-modal-title');
                const sub = document.getElementById('focus-modal-subtitle');
                if (title) title.innerText = session.activityTitle || 'Momento de Foco';
                if (sub) sub.innerText = `Categoria: ${session.category || 'Geral'} • ${session.plannedDurationMinutes} min planejados`;
            }
            modal.classList.remove('hidden');
        }
    }

    function closeFocusModal() {
        const modal = document.getElementById('modal-focus-session');
        if (modal) modal.classList.add('hidden');
    }

    function updateFocusSessionUI() {
        const session = Storage.getActiveFocusSession();
        const banner = document.getElementById('home-focus-card');
        if (!banner) return;

        if (!session) {
            banner.classList.add('hidden');
            return;
        }

        banner.classList.remove('hidden');
        const titleEl = document.getElementById('focus-banner-title');
        const statusEl = document.getElementById('focus-banner-status');
        const btnPause = document.getElementById('focus-banner-btn-pause');

        if (titleEl) titleEl.innerText = session.activityTitle;
        if (statusEl) {
            statusEl.innerText = session.isPaused ? 'Em pausa' : 'Em foco';
            statusEl.style.backgroundColor = session.isPaused ? 'rgba(245, 158, 11, 0.2)' : 'rgba(16, 185, 129, 0.2)';
            statusEl.style.color = session.isPaused ? '#D97706' : '#047857';
        }
        if (btnPause) {
            btnPause.innerText = session.isPaused ? 'Retomar' : 'Pausar';
        }

        updateFocusTimerTick();
    }

    function updateFocusTimerTick() {
        const session = Storage.getActiveFocusSession();
        if (!session) return;

        const now = Date.now();
        let elapsed = session.elapsedSecondsBeforePause;
        if (!session.isPaused) {
            elapsed += Math.max(0, Math.floor((now - session.startTimeMillis) / 1000));
        }

        const totalPlannedSec = session.plannedDurationMinutes * 60;
        const remainingSec = Math.max(0, totalPlannedSec - elapsed);
        const mins = Math.floor(remainingSec / 60);
        const secs = remainingSec % 60;
        const timeFormatted = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

        const timerBanner = document.getElementById('focus-banner-timer');
        const progressBanner = document.getElementById('focus-banner-progress');
        const timerModal = document.getElementById('focus-modal-timer');
        const statusBadgeModal = document.getElementById('focus-modal-status-badge');

        if (timerBanner) timerBanner.innerText = timeFormatted;
        if (timerModal) timerModal.innerText = timeFormatted;
        if (statusBadgeModal) {
            statusBadgeModal.innerText = session.isPaused ? 'Em pausa' : 'Em foco';
        }

        const ratio = totalPlannedSec > 0 ? Math.min(100, Math.round((elapsed / totalPlannedSec) * 100)) : 0;
        if (progressBanner) progressBanner.style.width = `${ratio}%`;
    }

    function updateDayTrackingUI(scheduledToday, avail) {
        const total = scheduledToday.length;
        const completed = scheduledToday.filter(e => e.status === 'COMPLETED').length;
        const postponed = scheduledToday.filter(e => e.status === 'POSTPONED').length;
        const pending = scheduledToday.filter(e => e.status === 'PENDING' || e.status === 'IN_PROGRESS').length;

        const ratioEl = document.getElementById('tracking-progress-ratio');
        const headerEl = document.getElementById('tracking-narrative-header');
        const subEl = document.getElementById('tracking-narrative-sub');
        const countComp = document.getElementById('tracking-count-completed');
        const countPend = document.getElementById('tracking-count-pending');
        const countPost = document.getElementById('tracking-count-postponed');

        if (ratioEl) ratioEl.innerText = `${completed}/${total}`;
        if (countComp) countComp.innerText = completed;
        if (countPend) countPend.innerText = pending;
        if (countPost) countPost.innerText = postponed;

        if (headerEl) {
            if (total === 0) headerEl.innerText = 'Seu dia ainda está livre';
            else if (completed === total && total > 0) headerEl.innerText = 'Todas as atividades concluídas!';
            else if (completed > 0) headerEl.innerText = 'Seu dia está indo bem!';
            else headerEl.innerText = 'Começando a rotina de hoje';
        }

        if (subEl) {
            let msg = total > 0 ? `Você concluiu ${completed} de ${total} atividades planejadas.` : 'Nenhuma atividade agendada ainda.';
            if (avail && avail.remainingFreeMinutes > 0) {
                msg += ` Restam ${formatMinutesToHours(avail.remainingFreeMinutes)} livres.`;
            }
            subEl.innerText = msg;
        }

        const nowMin = new Date().getHours() * 60 + new Date().getMinutes();
        const activeEntry = scheduledToday.find(e => e.status === 'IN_PROGRESS' || (e.status === 'PENDING' && nowMin >= (e.startHour * 60 + e.startMinute) && nowMin < (e.startHour * 60 + e.startMinute + e.targetDurationMinutes))) ||
            scheduledToday.find(e => e.status === 'PENDING' && (e.startHour * 60 + e.startMinute) >= nowMin) ||
            scheduledToday.find(e => e.status === 'PENDING');

        currentTrackingEntry = activeEntry || null;

        const kickerEl = document.getElementById('tracking-active-kicker');
        const titleEl = document.getElementById('tracking-active-title');
        const timeEl = document.getElementById('tracking-active-time');
        const actionsBox = document.getElementById('tracking-active-actions');

        if (activeEntry) {
            const isNow = activeEntry.status === 'IN_PROGRESS' || (nowMin >= (activeEntry.startHour * 60 + activeEntry.startMinute) && nowMin < (activeEntry.startHour * 60 + activeEntry.startMinute + activeEntry.targetDurationMinutes));
            if (kickerEl) kickerEl.innerText = isNow ? 'Acontecendo agora' : 'Próxima atividade';
            if (titleEl) titleEl.innerText = activeEntry.activityTitle;
            if (timeEl) timeEl.innerText = `${formatTime(activeEntry.startHour, activeEntry.startMinute)} (${activeEntry.targetDurationMinutes}m)`;
            if (actionsBox) actionsBox.classList.remove('hidden');
        } else {
            if (kickerEl) kickerEl.innerText = 'Status';
            if (titleEl) titleEl.innerText = 'Nenhuma atividade no momento';
            if (timeEl) timeEl.innerText = '--:--';
            if (actionsBox) actionsBox.classList.add('hidden');
        }
    }

    function openTimeExtensionModal(entryId) {
        activeExtensionEntryId = entryId;
        selectedExtensionMinutes = 15;
        const modal = document.getElementById('modal-time-extension');
        if (!modal) return;

        const entries = Storage.getAllScheduleEntries();
        const entry = entries.find(e => e.id === entryId);
        const titleEl = document.getElementById('ext-modal-task-title');
        if (titleEl && entry) {
            titleEl.innerText = `Estender "${entry.activityTitle}" (${formatTime(entry.startHour, entry.startMinute)})`;
        }

        checkExtensionConflict();
        modal.classList.remove('hidden');
    }

    function closeTimeExtensionModal() {
        const modal = document.getElementById('modal-time-extension');
        if (modal) modal.classList.add('hidden');
        activeExtensionEntryId = null;
    }

    function checkExtensionConflict() {
        const warningEl = document.getElementById('ext-conflict-warning');
        if (!warningEl || !activeExtensionEntryId) return;

        const entries = Storage.getAllScheduleEntries();
        const entry = entries.find(e => e.id === activeExtensionEntryId);
        if (!entry) return;

        const todayEntries = Storage.getScheduleEntries(entry.date || toISODate(new Date()));
        const startMin = entry.startHour * 60 + entry.startMinute;
        const projectedEnd = startMin + entry.targetDurationMinutes + selectedExtensionMinutes;

        const hasConflict = todayEntries.some(e => e.id !== entry.id && e.status === 'PENDING' && (e.startHour * 60 + e.startMinute) < projectedEnd && (e.startHour * 60 + e.startMinute) >= startMin);
        if (hasConflict) warningEl.classList.remove('hidden');
        else warningEl.classList.add('hidden');
    }

    function updateDayBalanceUI() {
        const todayStr = toISODate(new Date());
        const balance = Storage.getDayBalance(todayStr);

        const colStudies = document.getElementById('balance-col-studies');
        const colWork = document.getElementById('balance-col-work');
        const colLeisure = document.getElementById('balance-col-leisure');
        const colHome = document.getElementById('balance-col-home');
        const colRest = document.getElementById('balance-col-rest');
        const totalFocus = document.getElementById('balance-total-focus');
        const wbText = document.getElementById('balance-wellbeing-text');
        const wbIcon = document.getElementById('balance-wellbeing-icon');

        if (colStudies) colStudies.innerText = `${balance.studiesMinutes}m`;
        if (colWork) colWork.innerText = `${balance.workMinutes}m`;
        if (colLeisure) colLeisure.innerText = `${balance.leisureMinutes}m`;
        if (colHome) colHome.innerText = `${balance.homeMinutes}m`;
        if (colRest) colRest.innerText = `${balance.restMinutes}m`;
        if (totalFocus) totalFocus.innerText = `${balance.totalFocusMinutes} minutos`;

        if (wbText && wbIcon) {
            const productive = balance.studiesMinutes + balance.workMinutes;
            if (productive >= 480) {
                wbIcon.innerText = '⚠️';
                wbText.innerText = 'Seu dia tem mais de 8 horas de foco e trabalho. Reserve momentos para desacelerar.';
            } else if (balance.restMinutes === 0 && balance.leisureMinutes === 0 && productive > 120) {
                wbIcon.innerText = '🌿';
                wbText.innerText = 'Ainda não identificamos momentos de descanso hoje. Uma pausa de 15 min renova sua energia.';
            } else if (balance.completedTasksCount > 0 && balance.postponedTasksCount === 0) {
                wbIcon.innerText = '✨';
                wbText.innerText = 'Excelente ritmo hoje! Suas tarefas estão sendo concluídas com calma e consistência.';
            } else {
                wbIcon.innerText = '🌱';
                wbText.innerText = 'Suas atividades estão bem distribuídas. Mantenha um ritmo tranquilo e respeite seus limites.';
            }
        }
    }

    return {
        init,
        refreshHome,
        refreshActivities,
        refreshPlanning,
        refreshHabits,
        refreshSettings,
        setTheme,
        setOrganizationMode,
        openAddActivitySheet,
        openScheduleEntrySheet,
        openEditActivity,
        promptDeleteActivity,
        calculateDayAvailability,
        openFocusModal,
        closeFocusModal,
        openTimeExtensionModal,
        closeTimeExtensionModal
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    App.init();
});
