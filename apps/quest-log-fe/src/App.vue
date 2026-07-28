<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { authState, login, logout } from './auth'
import scholarImage from './assets/classes/scholar.png'
import codeMageImage from './assets/classes/code-mage.png'
import guardianImage from './assets/classes/guardian.png'
import strategistImage from './assets/classes/strategist.png'
import bossImage from './assets/questlog-forest-boss.png'
import bossBattleImage from './assets/bosses/forest-guardian-battle.png'
import {
  acceptDailyRecommendationsRequest,
  archiveGoalRequest,
  attackRaidAttemptRequest,
  completeWeeklyQuestRequest,
  completeTaskRequest,
  createDailyTaskRequest,
  createWeeklyQuestRequest,
  createGoalRequest,
  deleteTaskRequest,
  deleteWeeklyQuestRequest,
  fetchDashboard,
  fetchRecommendationHistoryRequest,
  previewDailyRecommendationsRequest,
  resolveRaidAttemptRequest,
  skipWeeklyQuestRequest,
  skipTaskRequest,
  startRaidAttemptRequest,
  taskUpdatePayload,
  weeklyQuestUpdatePayload,
  type BossRaid,
  type CharacterProfile,
  type CharacterProgressionEvent,
  type DailyTask,
  type Goal,
  type GoalProgressSummary,
  type RaidAttempt,
  type RecommendationDraft,
  type RecommendationHistory,
  type WeeklyQuest,
} from './dashboardApi'
import { deriveCharacterJob } from './questPersona'

const GOAL_STATUS = { active: 'ACTIVE' } as const
const TASK_STATUS = { completed: 'COMPLETED', pending: 'PENDING', skipped: 'SKIPPED' } as const
const TASK_FILTERS = ['ALL', ...Object.values(TASK_STATUS)] as const
const TABS = ['QUESTS', 'BOSS'] as const

type TaskFilter = (typeof TASK_FILTERS)[number]
type TabKey = (typeof TABS)[number]
type QuestEntry =
  | { kind: 'daily'; key: string; task: DailyTask }
  | { kind: 'weekly'; key: string; quest: WeeklyQuest }
type ReviewDraft = RecommendationDraft & { reviewId: number; selected: boolean }

const characterImages = {
  scholar: scholarImage,
  'code-mage': codeMageImage,
  guardian: guardianImage,
  strategist: strategistImage,
  pathfinder: codeMageImage,
} as const

function weekStartDate(dateValue: string) {
  const date = new Date(`${dateValue}T00:00:00`)
  const day = date.getDay()
  const mondayOffset = day === 0 ? -6 : 1 - day
  date.setDate(date.getDate() + mondayOffset)
  return date.toLocaleDateString('en-CA')
}

const today = new Date().toLocaleDateString('en-CA')
const selectedTaskDate = ref(today)
const currentWeekStartDate = computed(() => weekStartDate(selectedTaskDate.value))
const loading = ref(true)
const actionPending = ref(false)
const error = ref('')
const notice = ref('')
const goals = ref<Goal[]>([])
const goalProgressSummaries = ref<GoalProgressSummary[]>([])
const tasks = ref<DailyTask[]>([])
const persistedWeeklyQuests = ref<WeeklyQuest[]>([])
const character = ref<CharacterProfile | null>(null)
const progressionEvents = ref<CharacterProgressionEvent[]>([])
const raids = ref<BossRaid[]>([])
const attempts = ref<RaidAttempt[]>([])
const selectedGoalId = ref<number | null>(null)
const taskFilter = ref<TaskFilter>('ALL')
const activeTab = ref<TabKey>('QUESTS')
const goalModalOpen = ref(false)
const editingGoalId = ref<number | null>(null)
const goalDraft = ref({ title: '', description: '', targetDate: '' })
const editingTaskId = ref<number | null>(null)
const taskDraft = ref({ title: '', description: '', goalId: null as number | null, xpReward: 10 })
const recommendationDrafts = ref<ReviewDraft[]>([])
const recommendationGoalId = ref<number | null>(null)
const recommendationHistory = ref<RecommendationHistory[]>([])
const taskComposerOpen = ref(false)
const newTaskDraft = ref({ title: '', description: '', xpReward: 20 })
const weeklyQuestComposerOpen = ref(false)
const newWeeklyQuestDraft = ref({ title: '', description: '', xpReward: 80 })
const editingWeeklyQuestId = ref<number | null>(null)
const weeklyQuestDraft = ref({ title: '', description: '', goalId: null as number | null, xpReward: 40 })

const activeGoals = computed(() => goals.value.filter((goal) => goal.status === GOAL_STATUS.active))
const selectedGoal = computed(() => activeGoals.value.find((goal) => goal.id === selectedGoalId.value) ?? activeGoals.value[0] ?? null)
const taskGoalOptions = computed(() => goals.value.filter((goal) => goal.status === GOAL_STATUS.active || goal.id === taskDraft.value.goalId))
const weeklyQuestGoalOptions = computed(() => goals.value.filter((goal) => goal.status === GOAL_STATUS.active || goal.id === weeklyQuestDraft.value.goalId))
const progressSummaryByGoalId = computed(() => new Map(goalProgressSummaries.value.map((summary) => [summary.goalId, summary])))
const selectedGoalProgressSummary = computed(() => selectedGoal.value ? progressSummaryByGoalId.value.get(selectedGoal.value.id) ?? null : null)
const selectedGoalDailyTasks = computed(() => selectedGoal.value ? tasks.value.filter((task) => task.goalId === selectedGoal.value?.id) : tasks.value)
const weeklyQuests = computed(() => selectedGoal.value ? persistedWeeklyQuests.value.filter((quest) => quest.goalId === selectedGoal.value?.id) : persistedWeeklyQuests.value)
const filteredDailyTasks = computed(() => taskFilter.value === 'ALL' ? selectedGoalDailyTasks.value : selectedGoalDailyTasks.value.filter((task) => task.status === taskFilter.value))
const filteredWeeklyQuests = computed(() => taskFilter.value === 'ALL' ? weeklyQuests.value : weeklyQuests.value.filter((quest) => quest.status === taskFilter.value))
const questEntries = computed<QuestEntry[]>(() => [
  ...filteredDailyTasks.value.map((task) => ({ kind: 'daily' as const, key: `daily-${task.id}`, task })),
  ...filteredWeeklyQuests.value.map((quest) => ({ kind: 'weekly' as const, key: `weekly-${quest.id}`, quest })),
])
const taskFilterCounts = computed<Record<TaskFilter, number>>(() => {
  const counts: Record<TaskFilter, number> = { ALL: selectedGoalDailyTasks.value.length + weeklyQuests.value.length, PENDING: 0, COMPLETED: 0, SKIPPED: 0 }
  selectedGoalDailyTasks.value.forEach((task) => { counts[task.status] += 1 })
  weeklyQuests.value.forEach((quest) => { counts[quest.status] += 1 })
  return counts
})
const currentJob = computed(() => deriveCharacterJob(selectedGoal.value))
const currentCharacterImage = computed(() => characterImages[currentJob.value.key])
const jobClassName = computed(() => `job-${currentJob.value.key}`)
function characterImageFor(goal: Pick<Goal, 'title' | 'description'>) {
  return characterImages[deriveCharacterJob(goal).key]
}
function goalProgressLabel(goalId: number) {
  const summary = progressSummaryByGoalId.value.get(goalId)
  if (!summary) return 'No quests yet'
  return `${summary.completedQuestCount}/${summary.dailyQuestCount + summary.weeklyQuestCount} done`
}
const progressPercent = computed(() => character.value?.currentLevelXp ?? 0)
const clearedRaidIds = computed(() => new Set(attempts.value.filter((attempt) => attempt.status === 'CLEARED').map((attempt) => attempt.bossRaidId)))
const clearedRaidCount = computed(() => clearedRaidIds.value.size)
const activeAttemptByRaidId = computed(() => new Map(
  attempts.value
    .filter((attempt) => attempt.status === 'STARTED' || attempt.status === 'IN_PROGRESS')
    .map((attempt) => [attempt.bossRaidId, attempt]),
))
const pendingQuestCount = computed(() => selectedGoalDailyTasks.value.filter((task) => task.status === TASK_STATUS.pending).length + weeklyQuests.value.filter((quest) => quest.status === TASK_STATUS.pending).length)
const recentProgressionEvents = computed(() => progressionEvents.value.slice(0, 4))
const authEnabled = computed(() => authState.mode === 'keycloak')
const authLabel = computed(() => authState.authenticated ? authState.username || 'Authenticated user' : 'Signed out')
const hasRecommendationDrafts = computed(() => recommendationGoalId.value === selectedGoal.value?.id && recommendationDrafts.value.length > 0)
const selectedGoalRecommendationHistory = computed(() => selectedGoal.value ? recommendationHistory.value.filter((item) => item.goalId === selectedGoal.value?.id) : recommendationHistory.value)
const selectedRecommendationDrafts = computed(() => recommendationDrafts.value.filter((draft) => draft.selected))
const canCreateDailyTask = computed(() => Boolean(
  newTaskDraft.value.title.trim()
  && Number.isInteger(newTaskDraft.value.xpReward)
  && newTaskDraft.value.xpReward >= 1
  && newTaskDraft.value.xpReward <= 1000
))
const canCreateWeeklyQuest = computed(() => Boolean(
  newWeeklyQuestDraft.value.title.trim()
  && Number.isInteger(newWeeklyQuestDraft.value.xpReward)
  && newWeeklyQuestDraft.value.xpReward >= 1
  && newWeeklyQuestDraft.value.xpReward <= 5000
))
const canAcceptRecommendationDrafts = computed(() => selectedRecommendationDrafts.value.length > 0 && selectedRecommendationDrafts.value.every((draft) => (
  draft.title.trim()
  && Number.isInteger(draft.xpReward)
  && draft.xpReward >= 1
  && draft.xpReward <= 1000
)))
const isTodaySelected = computed(() => selectedTaskDate.value === today)

function apiMessage(caught: unknown) {
  if (axios.isAxiosError(caught)) return caught.response?.data?.message ?? 'The QuestLog backend is unavailable.'
  return 'An unexpected error occurred.'
}

function shiftedDate(dateValue: string, days: number) {
  const date = new Date(`${dateValue}T00:00:00`)
  date.setDate(date.getDate() + days)
  return date.toLocaleDateString('en-CA')
}

async function moveTaskDate(days: number) {
  selectedTaskDate.value = shiftedDate(selectedTaskDate.value, days)
  await loadDashboard()
}

async function selectToday() {
  if (isTodaySelected.value) return
  selectedTaskDate.value = today
  await loadDashboard()
}

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const dashboard = await fetchDashboard(selectedTaskDate.value)
    goals.value = dashboard.goals
    goalProgressSummaries.value = dashboard.goalProgressSummaries ?? []
    tasks.value = dashboard.dailyTasks
    persistedWeeklyQuests.value = dashboard.weeklyQuests
    character.value = dashboard.character
    progressionEvents.value = dashboard.progressionEvents ?? []
    raids.value = dashboard.raids
    attempts.value = dashboard.raidAttempts
    if ((selectedGoalId.value === null || !activeGoals.value.some((goal) => goal.id === selectedGoalId.value)) && activeGoals.value.length > 0) selectedGoalId.value = activeGoals.value[0]?.id ?? null
    await loadRecommendationHistory()
  } catch (caught) {
    error.value = apiMessage(caught)
  } finally {
    loading.value = false
  }
}

async function loadRecommendationHistory() {
  if (!selectedGoalId.value) {
    recommendationHistory.value = []
    return
  }
  recommendationHistory.value = await fetchRecommendationHistoryRequest(selectedGoalId.value, 6)
}

function openGoalModal(goal?: Goal) {
  editingGoalId.value = goal?.id ?? null
  goalDraft.value = goal ? { title: goal.title, description: goal.description ?? '', targetDate: goal.targetDate ?? '' } : { title: '', description: '', targetDate: '' }
  goalModalOpen.value = true
}
function closeGoalModal() { goalModalOpen.value = false; editingGoalId.value = null }

async function saveGoalFromModal() {
  if (!goalDraft.value.title.trim()) return
  if (editingGoalId.value) {
    const goal = goals.value.find((item) => item.id === editingGoalId.value)
    if (!goal) return
    await runAction(async () => {
      await axios.put(`/api/bff/goals/${goal.id}`, { title: goalDraft.value.title, description: goalDraft.value.description, status: goal.status, targetDate: goalDraft.value.targetDate || null })
      closeGoalModal()
    }, 'Goal updated.')
    return
  }
  await runAction(async () => {
    const createdGoal = await createGoalRequest({ title: goalDraft.value.title, description: goalDraft.value.description || 'System goal created from the setup popup', targetDate: goalDraft.value.targetDate || null, taskDate: selectedTaskDate.value })
    selectedGoalId.value = createdGoal.id
    closeGoalModal()
  }, 'Goal set. Daily quests were generated and weekly quests are ready.')
}

async function archiveGoal(goal: Goal) {
  await runAction(async () => { await archiveGoalRequest(goal); if (selectedGoalId.value === goal.id) selectedGoalId.value = null }, 'Goal archived.')
}
async function confirmArchiveGoal(goal: Goal) { await confirmAction(`Archive "${goal.title}"?`, () => archiveGoal(goal)) }
function recommendationNotice(recommendedTasks: DailyTask[]) {
  return recommendedTasks.length === 1
    ? '1 recommended daily quest is ready.'
    : `${recommendedTasks.length} recommended daily quests are ready.`
}
function reviewDrafts(drafts: RecommendationDraft[]) {
  return drafts.map((draft, index) => ({ ...draft, reviewId: index + 1, selected: true }))
}
async function previewDailyRecommendations(goal: Goal) {
  await runAction(async () => {
    const drafts = await previewDailyRecommendationsRequest(goal.id, selectedTaskDate.value)
    recommendationGoalId.value = goal.id
    recommendationDrafts.value = reviewDrafts(drafts)
    return drafts
  }, (drafts) => drafts.length === 1 ? 'Review 1 recommended quest before accepting it.' : `Review ${drafts.length} recommended quests before accepting them.`)
}
async function acceptDailyRecommendations() {
  if (!selectedGoal.value || !canAcceptRecommendationDrafts.value) return
  await runAction(async () => {
    const selectedDrafts = selectedRecommendationDrafts.value.map(({ reviewId, selected, ...draft }) => draft)
    const recommendedTasks = await acceptDailyRecommendationsRequest(selectedGoal.value!.id, selectedDrafts)
    recommendationDrafts.value = []
    recommendationGoalId.value = null
    return recommendedTasks
  }, recommendationNotice)
}
function dismissRecommendationDraft(reviewId: number) {
  recommendationDrafts.value = recommendationDrafts.value.filter((draft) => draft.reviewId !== reviewId)
  if (recommendationDrafts.value.length === 0) {
    recommendationGoalId.value = null
    notice.value = 'Recommended quests dismissed.'
  }
}
function dismissDailyRecommendations() {
  recommendationDrafts.value = []
  recommendationGoalId.value = null
  notice.value = 'Recommended quests dismissed.'
}

async function selectGoal(goalId: number) {
  selectedGoalId.value = goalId
  await loadRecommendationHistory()
}

function startTaskEdit(task: DailyTask) { editingTaskId.value = task.id; taskDraft.value = { title: task.title, description: task.description ?? '', goalId: task.goalId, xpReward: task.xpReward } }
function cancelTaskEdit() { editingTaskId.value = null }
function openTaskComposer() {
  taskComposerOpen.value = true
  newTaskDraft.value = { title: '', description: '', xpReward: 20 }
}
function closeTaskComposer() { taskComposerOpen.value = false }
function openWeeklyQuestComposer() {
  weeklyQuestComposerOpen.value = true
  newWeeklyQuestDraft.value = { title: '', description: '', xpReward: 80 }
}
function closeWeeklyQuestComposer() { weeklyQuestComposerOpen.value = false }
function isPendingTask(task: DailyTask) { return task.status === TASK_STATUS.pending }
function taskSourceLabel(task: DailyTask) { return task.status === TASK_STATUS.skipped ? 'SKIPPED' : task.source === 'AI_RECOMMENDED' ? 'SYSTEM DAILY' : 'MANUAL DAILY' }
function startWeeklyQuestEdit(quest: WeeklyQuest) { editingWeeklyQuestId.value = quest.id; weeklyQuestDraft.value = { title: quest.title, description: quest.description ?? '', goalId: quest.goalId, xpReward: quest.xpReward } }
function cancelWeeklyQuestEdit() { editingWeeklyQuestId.value = null }
function isPendingWeeklyQuest(quest: WeeklyQuest) { return quest.status === TASK_STATUS.pending }
function weeklyQuestSourceLabel(quest: WeeklyQuest) { return quest.status === TASK_STATUS.skipped ? 'SKIPPED' : quest.status === TASK_STATUS.completed ? 'COMPLETED' : quest.source === 'SYSTEM' ? 'SYSTEM WEEKLY' : 'MANUAL WEEKLY' }

async function saveTask(task: DailyTask) {
  if (!taskDraft.value.title.trim() || !Number.isInteger(taskDraft.value.xpReward) || taskDraft.value.xpReward < 1 || taskDraft.value.xpReward > 1000) return
  await runAction(async () => {
    await axios.put(`/api/bff/daily-tasks/${task.id}`, taskUpdatePayload(task, { goalId: taskDraft.value.goalId, title: taskDraft.value.title, description: taskDraft.value.description, xpReward: taskDraft.value.xpReward }))
    editingTaskId.value = null
  }, 'Daily task updated.')
}
async function createManualTask() {
  if (!canCreateDailyTask.value) return
  await runAction(async () => {
    await createDailyTaskRequest({
      goalId: selectedGoal.value?.id ?? null,
      title: newTaskDraft.value.title,
      description: newTaskDraft.value.description,
      taskDate: selectedTaskDate.value,
      xpReward: newTaskDraft.value.xpReward,
    })
    closeTaskComposer()
  }, 'Manual daily quest added.')
}
async function createManualWeeklyQuest() {
  if (!canCreateWeeklyQuest.value) return
  await runAction(async () => {
    await createWeeklyQuestRequest({
      goalId: selectedGoal.value?.id ?? null,
      title: newWeeklyQuestDraft.value.title,
      description: newWeeklyQuestDraft.value.description,
      weekStartDate: currentWeekStartDate.value,
      xpReward: newWeeklyQuestDraft.value.xpReward,
    })
    closeWeeklyQuestComposer()
  }, 'Manual weekly quest added.')
}
async function completeTask(task: DailyTask) { await runAction(async () => completeTaskRequest(task.id), (xpAwarded) => `Quest complete. +${xpAwarded} XP`) }
async function skipPendingTask(task: DailyTask) { await runAction(async () => { await skipTaskRequest(task) }, 'Daily task skipped.') }
async function deletePendingTask(taskId: number) { await runAction(async () => { await deleteTaskRequest(taskId) }, 'Daily task deleted.') }
async function confirmDeletePendingTask(task: DailyTask) { await confirmAction(`Delete "${task.title}"?`, () => deletePendingTask(task.id)) }
async function saveWeeklyQuest(quest: WeeklyQuest) {
  if (!weeklyQuestDraft.value.title.trim() || !Number.isInteger(weeklyQuestDraft.value.xpReward) || weeklyQuestDraft.value.xpReward < 1 || weeklyQuestDraft.value.xpReward > 5000) return
  await runAction(async () => {
    await axios.put(`/api/bff/weekly-quests/${quest.id}`, weeklyQuestUpdatePayload(quest, { goalId: weeklyQuestDraft.value.goalId, title: weeklyQuestDraft.value.title, description: weeklyQuestDraft.value.description, xpReward: weeklyQuestDraft.value.xpReward }))
    editingWeeklyQuestId.value = null
  }, 'Weekly quest updated.')
}
async function completeWeeklyQuest(quest: WeeklyQuest) { await runAction(async () => completeWeeklyQuestRequest(quest.id), (xpAwarded) => `Weekly quest complete. +${xpAwarded} XP`) }
async function skipPendingWeeklyQuest(quest: WeeklyQuest) { await runAction(async () => { await skipWeeklyQuestRequest(quest) }, 'Weekly quest skipped.') }
async function deletePendingWeeklyQuest(weeklyQuestId: number) { await runAction(async () => { await deleteWeeklyQuestRequest(weeklyQuestId) }, 'Weekly quest deleted.') }
async function confirmDeletePendingWeeklyQuest(quest: WeeklyQuest) { await confirmAction(`Delete "${quest.title}"?`, () => deletePendingWeeklyQuest(quest.id)) }
async function startRaid(raid: BossRaid) {
  await runAction(async () => startRaidAttemptRequest(raid.id), `${raid.name} started.`)
}
async function attackRaid(attempt: RaidAttempt) {
  await runAction(async () => attackRaidAttemptRequest(attempt.id), (result) => (
    result.status === 'CLEARED'
      ? `${result.bossName} cleared. +${result.xpAwarded} XP`
      : `${result.bossName} hit. ${result.bossRemainingHp} HP remains.`
  ))
}
async function resolveRaid(attempt: RaidAttempt) {
  await runAction(async () => resolveRaidAttemptRequest(attempt.id), (result) => `${result.bossName} attempt ended as ${result.status}.`)
}

async function runAction<T>(action: () => Promise<T>, successMessage: string | ((result: T) => string)) {
  actionPending.value = true
  error.value = ''
  notice.value = ''
  try {
    const result = await action()
    notice.value = typeof successMessage === 'function' ? successMessage(result) : successMessage
    await loadDashboard()
  } catch (caught) {
    error.value = apiMessage(caught)
  } finally {
    actionPending.value = false
  }
}
async function confirmAction(message: string, action: () => Promise<void>) { if (window.confirm(message)) await action() }
onMounted(() => { if (authState.authenticated) void loadDashboard(); else loading.value = false })
</script>

<template>
  <v-app>
    <v-main>
      <div class="dashboard-shell">
        <header class="topbar">
          <div class="brand-mark"><span>Q</span><strong>QuestLog</strong></div>
          <div class="topbar-actions">
            <div class="date-controls" aria-label="Dashboard quest date">
              <button class="date-step-button" type="button" :disabled="actionPending || loading" aria-label="Previous quest date" @click="moveTaskDate(-1)">&lt;</button>
              <input v-model="selectedTaskDate" type="date" aria-label="Dashboard quest date" :disabled="actionPending || loading" @change="loadDashboard" />
              <button class="date-step-button" type="button" :disabled="actionPending || loading" aria-label="Next quest date" @click="moveTaskDate(1)">&gt;</button>
              <button class="date-today-button" type="button" :disabled="actionPending || loading || isTodaySelected" @click="selectToday">Today</button>
            </div>
            <button v-if="authEnabled && !authState.authenticated" class="mini-button" type="button" @click="login">Log in</button>
            <button v-if="authEnabled && authState.authenticated" class="mini-button" type="button" @click="logout">Log out</button>
          </div>
        </header>

        <section class="hero-panel">
          <div class="hero-copy-block">
            <p class="eyebrow">AUTO-GENERATED RPG GOALS</p>
            <h1>Turn each goal into today&apos;s quest route.</h1>
            <p class="hero-copy">Set a goal and QuestLog prepares daily quests, weekly milestones, character progress, and boss raids in one dashboard.</p>
            <div class="hero-actions">
              <button class="primary-action" type="button" @click="openGoalModal()">Set goal</button>
              <button v-if="selectedGoal" class="ghost-action" type="button" @click="openGoalModal(selectedGoal)">Edit current goal</button>
            </div>
            <div class="metric-row">
              <div><span>ACTIVE GOALS</span><strong>{{ activeGoals.length }}</strong></div>
              <div><span>PENDING QUESTS</span><strong>{{ pendingQuestCount }}</strong></div>
              <div><span>CLEARED BOSSES</span><strong>{{ clearedRaidCount }}</strong></div>
            </div>
          </div>

          <aside class="character-card" :class="jobClassName">
            <div class="avatar-frame"><img :src="currentCharacterImage" :alt="`${currentJob.label} character portrait`" /></div>
            <div class="character-copy">
              <span class="level-badge">LV {{ character?.level ?? 1 }}</span>
              <h2>{{ currentJob.label }}</h2>
              <p>{{ selectedGoal?.title ?? 'Set a goal to define your class.' }}</p>
              <div class="xp-label"><span>{{ character?.displayName ?? 'Quest Hero' }}</span><strong>{{ character?.currentLevelXp ?? 0 }} / 100 XP</strong></div>
              <v-progress-linear :model-value="progressPercent" color="#9fe870" bg-color="#d8ead1" height="11" rounded />
              <div class="stat-row">
                <div><span>STR</span><strong>{{ character?.strength ?? 1 }}</strong></div>
                <div><span>VIT</span><strong>{{ character?.vitality ?? 1 }}</strong></div>
                <div><span>TOTAL</span><strong>{{ character?.totalXp ?? 0 }}</strong></div>
              </div>
              <div v-if="recentProgressionEvents.length > 0" class="progression-log">
                <p class="eyebrow">XP LOG</p>
                <div v-for="event in recentProgressionEvents" :key="event.id" class="progression-row">
                  <span>{{ event.sourceType.replace('_', ' ') }}</span>
                  <strong>+{{ event.xpAwarded }} XP</strong>
                  <small>LV {{ event.level }} / {{ event.totalXp }} total</small>
                </div>
              </div>
            </div>
          </aside>
        </section>

        <v-alert v-if="authState.error" type="warning" variant="tonal" class="mb-4">{{ authState.error }}</v-alert>
        <v-alert v-if="authEnabled && !authState.authenticated" type="info" variant="tonal" class="mb-4">Log in with Keycloak to load your QuestLog dashboard.</v-alert>
        <v-alert v-if="error" type="error" variant="tonal" closable class="mb-4">{{ error }}</v-alert>
        <v-alert v-if="notice" type="success" variant="tonal" closable class="mb-4">{{ notice }}</v-alert>

        <div v-if="loading" class="loading-grid"><v-skeleton-loader v-for="index in 2" :key="index" type="heading, paragraph, actions" /></div>

        <template v-else>
          <nav class="goal-rail" aria-label="Active goals">
            <button v-for="goal in activeGoals" :key="goal.id" type="button" class="goal-chip" :class="{ active: selectedGoal?.id === goal.id }" @click="selectGoal(goal.id)">
              <span>{{ goal.title }}</span><small>{{ deriveCharacterJob(goal).label }} / {{ goalProgressLabel(goal.id) }}</small>
            </button>
            <button type="button" class="goal-chip add-goal" @click="openGoalModal()"><span>+ Add goal</span><small>Auto-generate quests</small></button>
          </nav>

          <nav class="tab-bar" aria-label="QuestLog tabs">
            <button v-for="tab in TABS" :key="tab" type="button" :class="{ active: activeTab === tab }" @click="activeTab = tab">
              {{ tab === 'QUESTS' ? 'Quest board' : 'Boss raid' }}
            </button>
          </nav>

          <section v-if="activeTab === 'QUESTS'" class="tab-panel quest-layout">
            <aside class="focus-panel">
              <p class="eyebrow">CURRENT ROUTE</p>
              <h2>{{ selectedGoal?.title ?? 'Set a goal first.' }}</h2>
              <p>{{ selectedGoal?.description ?? 'Once a goal exists, QuestLog can prepare daily and weekly quests for it.' }}</p>
              <div class="focus-job"><span>{{ currentJob.label }}</span><small>{{ currentJob.subtitle }}</small></div>
              <div v-if="selectedGoalProgressSummary" class="goal-progress-summary">
                <div><span>COMPLETION</span><strong>{{ selectedGoalProgressSummary.completionRate }}%</strong></div>
                <div><span>QUESTS</span><strong>{{ selectedGoalProgressSummary.completedQuestCount }}/{{ selectedGoalProgressSummary.dailyQuestCount + selectedGoalProgressSummary.weeklyQuestCount }}</strong></div>
                <div><span>XP</span><strong>{{ selectedGoalProgressSummary.earnedXp }}/{{ selectedGoalProgressSummary.availableXp }}</strong></div>
              </div>
              <div class="panel-actions">
                <button v-if="selectedGoal" class="text-button" type="button" @click="openGoalModal(selectedGoal)">Edit Goal</button>
                <button v-if="selectedGoal" class="text-button" type="button" :disabled="actionPending" @click="previewDailyRecommendations(selectedGoal)">Generate Quests</button>
                <button v-if="selectedGoal" class="text-button muted" type="button" @click="confirmArchiveGoal(selectedGoal)">Archive</button>
              </div>
              <div v-if="selectedGoalRecommendationHistory.length > 0" class="recommendation-history">
                <p class="eyebrow">AI ACTIVITY</p>
                <div v-for="item in selectedGoalRecommendationHistory" :key="item.id" class="history-row">
                  <span>{{ item.action }}</span>
                  <strong>{{ item.title }}</strong>
                  <small>+{{ item.xpReward }} XP</small>
                </div>
              </div>
            </aside>

            <main class="quest-board">
              <div class="board-header">
                <div><p class="eyebrow">{{ selectedTaskDate }} / WEEK OF {{ currentWeekStartDate }}</p><h2>{{ isTodaySelected ? "Today's quests" : 'Selected day quests' }}</h2></div>
                <div class="board-tools">
                  <div class="composer-buttons">
                    <button class="text-button add-task-button" type="button" @click="openTaskComposer">Add daily quest</button>
                    <button class="text-button add-task-button" type="button" @click="openWeeklyQuestComposer">Add weekly quest</button>
                  </div>
                  <div class="filter-bar" aria-label="Daily task status filter">
                    <button v-for="filter in TASK_FILTERS" :key="filter" type="button" :class="{ active: taskFilter === filter }" :aria-pressed="taskFilter === filter" @click="taskFilter = filter">{{ filter }} {{ taskFilterCounts[filter] }}</button>
                  </div>
                </div>
              </div>

              <div class="quest-list">
                <form v-if="taskComposerOpen" class="manual-task-composer" @submit.prevent="createManualTask">
                  <div>
                    <p class="eyebrow">MANUAL DAILY</p>
                    <input v-model="newTaskDraft.title" maxlength="200" aria-label="New daily quest title" placeholder="Name the next concrete action" />
                  </div>
                  <input v-model="newTaskDraft.description" aria-label="New daily quest description" placeholder="Optional context" />
                  <input v-model.number="newTaskDraft.xpReward" class="composer-xp-input" type="number" min="1" max="1000" aria-label="New daily quest XP reward" />
                  <div class="edit-actions">
                    <button type="submit" :disabled="actionPending || !canCreateDailyTask">Add</button>
                    <button type="button" class="secondary-button" @click="closeTaskComposer">Cancel</button>
                  </div>
                </form>
                <form v-if="weeklyQuestComposerOpen" class="manual-task-composer weekly-composer" @submit.prevent="createManualWeeklyQuest">
                  <div>
                    <p class="eyebrow">MANUAL WEEKLY</p>
                    <input v-model="newWeeklyQuestDraft.title" maxlength="200" aria-label="New weekly quest title" placeholder="Name this week's milestone" />
                  </div>
                  <input v-model="newWeeklyQuestDraft.description" aria-label="New weekly quest description" placeholder="Optional success criteria" />
                  <input v-model.number="newWeeklyQuestDraft.xpReward" class="composer-xp-input" type="number" min="1" max="5000" aria-label="New weekly quest XP reward" />
                  <div class="edit-actions">
                    <button type="submit" :disabled="actionPending || !canCreateWeeklyQuest">Add</button>
                    <button type="button" class="secondary-button" @click="closeWeeklyQuestComposer">Cancel</button>
                  </div>
                </form>
                <div v-if="hasRecommendationDrafts" class="recommendation-review">
                  <div class="review-heading">
                    <div><p class="eyebrow">RECOMMENDATION REVIEW</p><h3>Preview daily quests before creation</h3></div>
                    <div class="review-actions">
                      <button class="text-button" type="button" :disabled="actionPending || !canAcceptRecommendationDrafts" @click="acceptDailyRecommendations">Accept selected</button>
                      <button class="text-button muted" type="button" :disabled="actionPending" @click="dismissDailyRecommendations">Dismiss</button>
                    </div>
                  </div>
                  <div v-for="draft in recommendationDrafts" :key="draft.reviewId" class="draft-card">
                    <label class="draft-select">
                      <input v-model="draft.selected" type="checkbox" :aria-label="`Select ${draft.title}`" />
                      <span>SYSTEM DAILY DRAFT</span>
                    </label>
                    <div class="draft-fields">
                      <input v-model="draft.title" maxlength="200" aria-label="Recommendation title" />
                      <input v-model="draft.description" aria-label="Recommendation description" />
                    </div>
                    <input v-model.number="draft.xpReward" class="draft-xp-input" type="number" min="1" max="1000" aria-label="Recommendation XP reward" />
                    <button class="text-button muted" type="button" :disabled="actionPending" @click="dismissRecommendationDraft(draft.reviewId)">Reject</button>
                  </div>
                </div>
                <div v-for="entry in questEntries" :key="entry.key" class="quest-card" :class="entry.kind">
                  <template v-if="entry.kind === 'daily'">
                    <form v-if="editingTaskId === entry.task.id" class="edit-form task-edit-form" @submit.prevent="saveTask(entry.task)">
                      <input v-model="taskDraft.title" maxlength="200" aria-label="Task title" />
                      <input v-model="taskDraft.description" aria-label="Task description" />
                      <select v-model="taskDraft.goalId" aria-label="Task goal"><option :value="null">No goal</option><option v-for="goal in taskGoalOptions" :key="goal.id" :value="goal.id">{{ goal.title }}</option></select>
                      <input v-model.number="taskDraft.xpReward" type="number" min="1" max="1000" aria-label="Task XP reward" />
                      <div class="edit-actions"><button type="submit" :disabled="actionPending || !taskDraft.title.trim() || !Number.isInteger(taskDraft.xpReward) || taskDraft.xpReward < 1 || taskDraft.xpReward > 1000">Save</button><button type="button" class="secondary-button" @click="cancelTaskEdit">Cancel</button></div>
                    </form>
                    <template v-else>
                      <button class="complete-button" :disabled="actionPending || entry.task.status !== TASK_STATUS.pending" :aria-label="`Complete ${entry.task.title}`" @click="completeTask(entry.task)"><span v-if="entry.task.status === TASK_STATUS.completed">OK</span><span v-else>D</span></button>
                      <div class="quest-copy"><strong>{{ entry.task.title }}</strong><span>{{ taskSourceLabel(entry.task) }}</span><p>{{ entry.task.description }}</p></div>
                      <b>+{{ entry.task.xpReward }} XP</b>
                      <button v-if="isPendingTask(entry.task)" class="text-button muted" :disabled="actionPending" @click="skipPendingTask(entry.task)">Skip</button>
                      <button v-if="isPendingTask(entry.task)" class="text-button" :disabled="actionPending" @click="startTaskEdit(entry.task)">Edit</button>
                      <button v-if="isPendingTask(entry.task)" class="task-delete-button" :disabled="actionPending" :aria-label="`Delete ${entry.task.title}`" @click="confirmDeletePendingTask(entry.task)">Delete</button>
                    </template>
                  </template>
                  <template v-else>
                    <form v-if="editingWeeklyQuestId === entry.quest.id" class="edit-form task-edit-form" @submit.prevent="saveWeeklyQuest(entry.quest)">
                      <input v-model="weeklyQuestDraft.title" maxlength="200" aria-label="Weekly quest title" />
                      <input v-model="weeklyQuestDraft.description" aria-label="Weekly quest description" />
                      <select v-model="weeklyQuestDraft.goalId" aria-label="Weekly quest goal"><option :value="null">No goal</option><option v-for="goal in weeklyQuestGoalOptions" :key="goal.id" :value="goal.id">{{ goal.title }}</option></select>
                      <input v-model.number="weeklyQuestDraft.xpReward" type="number" min="1" max="5000" aria-label="Weekly quest XP reward" />
                      <div class="edit-actions"><button type="submit" :disabled="actionPending || !weeklyQuestDraft.title.trim() || !Number.isInteger(weeklyQuestDraft.xpReward) || weeklyQuestDraft.xpReward < 1 || weeklyQuestDraft.xpReward > 5000">Save</button><button type="button" class="secondary-button" @click="cancelWeeklyQuestEdit">Cancel</button></div>
                    </form>
                    <template v-else>
                      <button class="complete-button" :disabled="actionPending || !isPendingWeeklyQuest(entry.quest)" :aria-label="`Complete ${entry.quest.title}`" @click="completeWeeklyQuest(entry.quest)"><span v-if="entry.quest.status === TASK_STATUS.completed">OK</span><span v-else>W</span></button>
                      <div class="quest-copy"><strong>{{ entry.quest.title }}</strong><span>{{ weeklyQuestSourceLabel(entry.quest) }}</span><p>{{ entry.quest.description }}</p></div><b>+{{ entry.quest.xpReward }} XP</b>
                      <button v-if="isPendingWeeklyQuest(entry.quest)" class="text-button muted" :disabled="actionPending" @click="skipPendingWeeklyQuest(entry.quest)">Skip</button>
                      <button v-if="isPendingWeeklyQuest(entry.quest)" class="text-button" :disabled="actionPending" @click="startWeeklyQuestEdit(entry.quest)">Edit</button>
                      <button v-if="isPendingWeeklyQuest(entry.quest)" class="task-delete-button" :disabled="actionPending" :aria-label="`Delete ${entry.quest.title}`" @click="confirmDeletePendingWeeklyQuest(entry.quest)">Delete</button>
                    </template>
                  </template>
                </div>
                <p v-if="questEntries.length === 0" class="empty-copy">No quests are available for the selected goal yet.</p>
              </div>
            </main>
          </section>

          <section v-else class="tab-panel boss-panel">
            <div class="boss-arena">
              <img :src="bossBattleImage" alt="Forest guardian battle arena" />
              <div class="boss-overlay">
                <p class="eyebrow">LIVE RAID ENCOUNTER</p>
                <h2>Guardian of the greenwood</h2>
                <p>Complete quests, grow your character, and clear unlocked boss stages.</p>
                <div class="battle-hud"><span>HERO LV {{ character?.level ?? 1 }}</span><span>{{ clearedRaidCount }} CLEARED</span></div>
              </div>
            </div>
            <div class="raid-list">
              <div v-for="raid in raids" :key="raid.id" class="raid-card">
                <img :src="bossImage" alt="Boss raid" />
                <div class="stage">0{{ raid.stage }}</div>
                <div class="raid-copy">
                  <strong>{{ raid.name }}</strong>
                  <p>Level {{ raid.requiredLevel }} / {{ raid.maxHp }} HP / +{{ raid.xpReward }} XP</p>
                  <div class="boss-hp">
                    <span :style="{ width: `${activeAttemptByRaidId.get(raid.id) ? Math.max(0, Math.round((activeAttemptByRaidId.get(raid.id)!.bossRemainingHp / raid.maxHp) * 100)) : raid.unlocked ? 100 : 28}%` }"></span>
                  </div>
                  <small v-if="activeAttemptByRaidId.get(raid.id)" class="raid-progress">
                    {{ activeAttemptByRaidId.get(raid.id)!.bossRemainingHp }} HP remains / {{ activeAttemptByRaidId.get(raid.id)!.damageDealt }} damage dealt
                  </small>
                </div>
                <div v-if="raid.unlocked && activeAttemptByRaidId.get(raid.id)" class="raid-actions">
                  <button class="raid-button" :disabled="actionPending" @click="attackRaid(activeAttemptByRaidId.get(raid.id)!)">Attack</button>
                  <button class="raid-button secondary" :disabled="actionPending" @click="resolveRaid(activeAttemptByRaidId.get(raid.id)!)">Withdraw</button>
                </div>
                <button v-else-if="raid.unlocked && !clearedRaidIds.has(raid.id)" class="raid-button" :disabled="actionPending" @click="startRaid(raid)">Start raid</button>
                <span v-else :class="['raid-state', clearedRaidIds.has(raid.id) ? 'cleared' : 'locked']">{{ clearedRaidIds.has(raid.id) ? 'CLEARED' : 'LOCKED' }}</span>
              </div>
            </div>
          </section>
        </template>

        <footer>{{ authEnabled ? `Keycloak / ${authLabel}` : 'Development mode / temporary dev-user identity' }}</footer>
      </div>

      <div v-if="goalModalOpen" class="modal-backdrop" role="presentation" @click.self="closeGoalModal">
        <section class="goal-modal" role="dialog" aria-modal="true" aria-labelledby="goal-modal-title">
          <div class="panel-heading"><div><p class="eyebrow">GOAL SETUP</p><h2 id="goal-modal-title">{{ editingGoalId ? 'Edit goal' : 'Set a new goal' }}</h2></div><button class="icon-button" type="button" aria-label="Close goal popup" @click="closeGoalModal">x</button></div>
          <form class="goal-form" @submit.prevent="saveGoalFromModal">
            <label>Goal title<input v-model="goalDraft.title" placeholder="Ship QuestLog, pass SQLP, build strength" maxlength="200" /></label>
            <label>Description<input v-model="goalDraft.description" placeholder="Add context so classes and quests fit the goal." /></label>
            <label>Target date<input v-model="goalDraft.targetDate" type="date" /></label>
            <div class="modal-preview" :class="`job-${deriveCharacterJob(goalDraft).key}`"><img :src="characterImageFor(goalDraft)" :alt="`${deriveCharacterJob(goalDraft).label} character preview`" /><div><strong>{{ deriveCharacterJob(goalDraft).label }}</strong><span>{{ deriveCharacterJob(goalDraft).subtitle }}</span></div></div>
            <div class="edit-actions modal-actions"><button type="submit" :disabled="actionPending || !goalDraft.title.trim()">{{ editingGoalId ? 'Save goal' : 'Create goal + auto quests' }}</button><button type="button" class="secondary-button" @click="closeGoalModal">Cancel</button></div>
          </form>
        </section>
      </div>
    </v-main>
  </v-app>
</template>

<style scoped>
.date-controls {
  display: grid;
  grid-template-columns: 38px minmax(144px, 172px) 38px auto;
  gap: 6px;
  align-items: center;
}

.date-controls input {
  min-height: 42px;
  padding: 9px 11px;
  border-radius: 999px;
  color: var(--deep);
  font: 800 12px 'DM Mono', monospace;
}

.date-step-button,
.date-today-button {
  min-height: 42px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--deep);
  background: rgba(255, 255, 255, .72);
  font: 900 12px 'DM Mono', monospace;
}

.date-step-button {
  width: 38px;
  padding: 0;
}

.date-today-button {
  padding: 0 13px;
}

.goal-progress-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: -8px 0 20px;
}

.goal-progress-summary div {
  min-width: 0;
  padding: 11px 10px;
  border: 1px solid rgba(14, 15, 12, .1);
  border-radius: 18px;
  background: rgba(255, 255, 255, .62);
}

.goal-progress-summary span {
  display: block;
  color: var(--muted);
  font: 700 9px 'DM Mono', monospace;
}

.goal-progress-summary strong {
  display: block;
  overflow: hidden;
  margin-top: 2px;
  color: var(--deep);
  font: 900 16px 'DM Mono', monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.raid-actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 8px;
}

.raid-button.secondary {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, .72);
  box-shadow: none;
}

.raid-progress {
  display: block;
  margin-top: 7px;
  color: var(--deep);
  font: 800 10px 'DM Mono', monospace;
}

.boss-hp {
  overflow: hidden;
  width: min(100%, 260px);
  height: 9px;
  margin-top: 9px;
  border-radius: 999px;
  background: #edf6e8;
}

.boss-hp span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #65b82f, var(--green));
  transition: width .28s ease;
}

@media (max-width: 700px) {
  .date-controls {
    width: 100%;
    grid-template-columns: 38px minmax(0, 1fr) 38px auto;
  }

  .raid-actions {
    width: 100%;
  }

  .raid-actions .raid-button {
    flex: 1 1 140px;
  }

  .boss-hp {
    width: 100%;
  }
}
</style>
