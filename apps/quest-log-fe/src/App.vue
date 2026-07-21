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
  archiveGoalRequest,
  completeTaskRequest,
  createGoalRequest,
  deleteTaskRequest,
  fetchDashboard,
  skipTaskRequest,
  taskUpdatePayload,
  type BossRaid,
  type CharacterProfile,
  type DailyTask,
  type Goal,
  type RaidAttempt,
} from './dashboardApi'
import { buildWeeklyQuests, deriveCharacterJob, type WeeklyQuest } from './questPersona'

const GOAL_STATUS = { active: 'ACTIVE' } as const
const TASK_STATUS = { completed: 'COMPLETED', pending: 'PENDING', skipped: 'SKIPPED' } as const
const TASK_FILTERS = ['ALL', ...Object.values(TASK_STATUS)] as const
const TABS = ['QUESTS', 'BOSS'] as const

type TaskFilter = (typeof TASK_FILTERS)[number]
type TabKey = (typeof TABS)[number]
type QuestEntry =
  | { kind: 'daily'; key: string; task: DailyTask }
  | { kind: 'weekly'; key: string; quest: WeeklyQuest }

const characterImages = {
  scholar: scholarImage,
  'code-mage': codeMageImage,
  guardian: guardianImage,
  strategist: strategistImage,
  pathfinder: codeMageImage,
} as const

const today = new Date().toLocaleDateString('en-CA')
const loading = ref(true)
const actionPending = ref(false)
const error = ref('')
const notice = ref('')
const goals = ref<Goal[]>([])
const tasks = ref<DailyTask[]>([])
const character = ref<CharacterProfile | null>(null)
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

const activeGoals = computed(() => goals.value.filter((goal) => goal.status === GOAL_STATUS.active))
const selectedGoal = computed(() => activeGoals.value.find((goal) => goal.id === selectedGoalId.value) ?? activeGoals.value[0] ?? null)
const taskGoalOptions = computed(() => goals.value.filter((goal) => goal.status === GOAL_STATUS.active || goal.id === taskDraft.value.goalId))
const selectedGoalDailyTasks = computed(() => selectedGoal.value ? tasks.value.filter((task) => task.goalId === selectedGoal.value?.id) : tasks.value)
const weeklyQuests = computed(() => buildWeeklyQuests(selectedGoal.value))
const filteredDailyTasks = computed(() => taskFilter.value === 'ALL' ? selectedGoalDailyTasks.value : selectedGoalDailyTasks.value.filter((task) => task.status === taskFilter.value))
const questEntries = computed<QuestEntry[]>(() => [
  ...filteredDailyTasks.value.map((task) => ({ kind: 'daily' as const, key: `daily-${task.id}`, task })),
  ...(taskFilter.value === 'ALL' || taskFilter.value === 'PENDING'
    ? weeklyQuests.value.map((quest) => ({ kind: 'weekly' as const, key: quest.id, quest }))
    : []),
])
const taskFilterCounts = computed<Record<TaskFilter, number>>(() => {
  const counts: Record<TaskFilter, number> = { ALL: selectedGoalDailyTasks.value.length + weeklyQuests.value.length, PENDING: weeklyQuests.value.length, COMPLETED: 0, SKIPPED: 0 }
  selectedGoalDailyTasks.value.forEach((task) => { counts[task.status] += 1 })
  return counts
})
const currentJob = computed(() => deriveCharacterJob(selectedGoal.value))
const currentCharacterImage = computed(() => characterImages[currentJob.value.key])
const jobClassName = computed(() => `job-${currentJob.value.key}`)
function characterImageFor(goal: Pick<Goal, 'title' | 'description'>) {
  return characterImages[deriveCharacterJob(goal).key]
}
const progressPercent = computed(() => character.value?.currentLevelXp ?? 0)
const clearedRaidIds = computed(() => new Set(attempts.value.filter((attempt) => attempt.status === 'VICTORY').map((attempt) => attempt.bossRaidId)))
const clearedRaidCount = computed(() => clearedRaidIds.value.size)
const pendingQuestCount = computed(() => selectedGoalDailyTasks.value.filter((task) => task.status === TASK_STATUS.pending).length + weeklyQuests.value.length)
const authEnabled = computed(() => authState.mode === 'keycloak')
const authLabel = computed(() => authState.authenticated ? authState.username || 'Authenticated user' : 'Signed out')

function apiMessage(caught: unknown) {
  if (axios.isAxiosError(caught)) return caught.response?.data?.message ?? 'The QuestLog backend is unavailable.'
  return 'An unexpected error occurred.'
}

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const dashboard = await fetchDashboard(today)
    goals.value = dashboard.goals
    tasks.value = dashboard.dailyTasks
    character.value = dashboard.character
    raids.value = dashboard.raids
    attempts.value = dashboard.raidAttempts
    if ((selectedGoalId.value === null || !activeGoals.value.some((goal) => goal.id === selectedGoalId.value)) && activeGoals.value.length > 0) selectedGoalId.value = activeGoals.value[0]?.id ?? null
  } catch (caught) {
    error.value = apiMessage(caught)
  } finally {
    loading.value = false
  }
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
    const createdGoal = await createGoalRequest({ title: goalDraft.value.title, description: goalDraft.value.description || 'System goal created from the setup popup', targetDate: goalDraft.value.targetDate || null, taskDate: today })
    selectedGoalId.value = createdGoal.id
    closeGoalModal()
  }, 'Goal set. Daily quests were generated and weekly quests are ready.')
}

async function archiveGoal(goal: Goal) {
  await runAction(async () => { await archiveGoalRequest(goal); if (selectedGoalId.value === goal.id) selectedGoalId.value = null }, 'Goal archived.')
}
async function confirmArchiveGoal(goal: Goal) { await confirmAction(`Archive "${goal.title}"?`, () => archiveGoal(goal)) }

function startTaskEdit(task: DailyTask) { editingTaskId.value = task.id; taskDraft.value = { title: task.title, description: task.description ?? '', goalId: task.goalId, xpReward: task.xpReward } }
function cancelTaskEdit() { editingTaskId.value = null }
function isPendingTask(task: DailyTask) { return task.status === TASK_STATUS.pending }
function taskSourceLabel(task: DailyTask) { return task.status === TASK_STATUS.skipped ? 'SKIPPED' : task.source === 'AI_RECOMMENDED' ? 'SYSTEM DAILY' : 'MANUAL DAILY' }

async function saveTask(task: DailyTask) {
  if (!taskDraft.value.title.trim() || !Number.isInteger(taskDraft.value.xpReward) || taskDraft.value.xpReward < 1 || taskDraft.value.xpReward > 1000) return
  await runAction(async () => {
    await axios.put(`/api/bff/daily-tasks/${task.id}`, taskUpdatePayload(task, { goalId: taskDraft.value.goalId, title: taskDraft.value.title, description: taskDraft.value.description, xpReward: taskDraft.value.xpReward }))
    editingTaskId.value = null
  }, 'Daily task updated.')
}
async function completeTask(task: DailyTask) { await runAction(async () => completeTaskRequest(task.id), (xpAwarded) => `Quest complete. +${xpAwarded} XP`) }
async function skipPendingTask(task: DailyTask) { await runAction(async () => { await skipTaskRequest(task) }, 'Daily task skipped.') }
async function deletePendingTask(taskId: number) { await runAction(async () => { await deleteTaskRequest(taskId) }, 'Daily task deleted.') }
async function confirmDeletePendingTask(task: DailyTask) { await confirmAction(`Delete "${task.title}"?`, () => deletePendingTask(task.id)) }
async function attemptRaid(raid: BossRaid) { await runAction(async () => (await axios.post<{ xpAwarded: number }>(`/api/bff/boss-raids/${raid.id}/attempts`)).data.xpAwarded, (xpAwarded) => `${raid.name} cleared. +${xpAwarded} XP`) }

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
            <span class="date-pill">{{ today }}</span>
            <button v-if="authEnabled && !authState.authenticated" class="mini-button" type="button" @click="login">Log in</button>
            <button v-if="authEnabled && authState.authenticated" class="mini-button" type="button" @click="logout">Log out</button>
          </div>
        </header>

        <section class="hero-panel">
          <div class="hero-copy-block">
            <p class="eyebrow">AUTO-GENERATED RPG GOALS</p>
            <h1>목표 하나가 오늘의 던전이 됩니다.</h1>
            <p class="hero-copy">Goal을 설정하면 시스템이 일일 퀘스트를 생성하고, 주간 퀘스트와 보스 루트까지 한 화면에서 관리합니다.</p>
            <div class="hero-actions">
              <button class="primary-action" type="button" @click="openGoalModal()">새 목표 설정</button>
              <button v-if="selectedGoal" class="ghost-action" type="button" @click="openGoalModal(selectedGoal)">현재 목표 수정</button>
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
              <p>{{ selectedGoal?.title ?? '목표를 설정하면 직업이 정해집니다.' }}</p>
              <div class="xp-label"><span>{{ character?.displayName ?? 'Quest Hero' }}</span><strong>{{ character?.currentLevelXp ?? 0 }} / 100 XP</strong></div>
              <v-progress-linear :model-value="progressPercent" color="#9fe870" bg-color="#d8ead1" height="11" rounded />
              <div class="stat-row">
                <div><span>STR</span><strong>{{ character?.strength ?? 1 }}</strong></div>
                <div><span>VIT</span><strong>{{ character?.vitality ?? 1 }}</strong></div>
                <div><span>TOTAL</span><strong>{{ character?.totalXp ?? 0 }}</strong></div>
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
            <button v-for="goal in activeGoals" :key="goal.id" type="button" class="goal-chip" :class="{ active: selectedGoal?.id === goal.id }" @click="selectedGoalId = goal.id">
              <span>{{ goal.title }}</span><small>{{ deriveCharacterJob(goal).label }}</small>
            </button>
            <button type="button" class="goal-chip add-goal" @click="openGoalModal()"><span>+ 목표 추가</span><small>자동 퀘스트 생성</small></button>
          </nav>

          <nav class="tab-bar" aria-label="QuestLog tabs">
            <button v-for="tab in TABS" :key="tab" type="button" :class="{ active: activeTab === tab }" @click="activeTab = tab">
              {{ tab === 'QUESTS' ? '퀘스트 보드' : '보스 레이드' }}
            </button>
          </nav>

          <section v-if="activeTab === 'QUESTS'" class="tab-panel quest-layout">
            <aside class="focus-panel">
              <p class="eyebrow">CURRENT ROUTE</p>
              <h2>{{ selectedGoal?.title ?? 'Goal을 먼저 설정하세요' }}</h2>
              <p>{{ selectedGoal?.description ?? '목표가 있어야 일일/주간 퀘스트가 자동 생성됩니다.' }}</p>
              <div class="focus-job"><span>{{ currentJob.label }}</span><small>{{ currentJob.subtitle }}</small></div>
              <div class="panel-actions">
                <button v-if="selectedGoal" class="text-button" type="button" @click="openGoalModal(selectedGoal)">Edit Goal</button>
                <button v-if="selectedGoal" class="text-button muted" type="button" @click="confirmArchiveGoal(selectedGoal)">Archive</button>
              </div>
            </aside>

            <main class="quest-board">
              <div class="board-header">
                <div><p class="eyebrow">{{ today }} / SYSTEM BOARD</p><h2>오늘의 퀘스트</h2></div>
                <div class="filter-bar" aria-label="Daily task status filter">
                  <button v-for="filter in TASK_FILTERS" :key="filter" type="button" :class="{ active: taskFilter === filter }" :aria-pressed="taskFilter === filter" @click="taskFilter = filter">{{ filter }} {{ taskFilterCounts[filter] }}</button>
                </div>
              </div>

              <div class="quest-list">
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
                      <button class="complete-button" :disabled="actionPending || entry.task.status !== TASK_STATUS.pending" :aria-label="`Complete ${entry.task.title}`" @click="completeTask(entry.task)"><span v-if="entry.task.status === TASK_STATUS.completed">✓</span></button>
                      <div class="quest-copy"><strong>{{ entry.task.title }}</strong><span>{{ taskSourceLabel(entry.task) }}</span><p>{{ entry.task.description }}</p></div>
                      <b>+{{ entry.task.xpReward }} XP</b>
                      <button v-if="isPendingTask(entry.task)" class="text-button muted" :disabled="actionPending" @click="skipPendingTask(entry.task)">Skip</button>
                      <button v-if="isPendingTask(entry.task)" class="text-button" :disabled="actionPending" @click="startTaskEdit(entry.task)">Edit</button>
                      <button v-if="isPendingTask(entry.task)" class="task-delete-button" :disabled="actionPending" :aria-label="`Delete ${entry.task.title}`" @click="confirmDeletePendingTask(entry.task)">Delete</button>
                    </template>
                  </template>
                  <template v-else>
                    <div class="weekly-icon">W</div><div class="quest-copy"><strong>{{ entry.quest.title }}</strong><span>SYSTEM WEEKLY</span><p>{{ entry.quest.description }}</p></div><b>+{{ entry.quest.xpReward }} XP</b>
                  </template>
                </div>
                <p v-if="questEntries.length === 0" class="empty-copy">선택한 목표에 표시할 퀘스트가 없습니다.</p>
              </div>
            </main>
          </section>

          <section v-else class="tab-panel boss-panel">
            <div class="boss-arena">
              <img :src="bossBattleImage" alt="Forest guardian battle arena" />
              <div class="boss-overlay">
                <p class="eyebrow">LIVE RAID ENCOUNTER</p>
                <h2>연초록 숲의 수호자</h2>
                <p>퀘스트를 완료해 성장하고, 잠금 해제된 보스를 격파하세요.</p>
                <div class="battle-hud"><span>HERO LV {{ character?.level ?? 1 }}</span><span>{{ clearedRaidCount }} CLEARED</span></div>
              </div>
            </div>
            <div class="raid-list"><div v-for="raid in raids" :key="raid.id" class="raid-card"><img :src="bossImage" alt="Boss raid" /><div class="stage">0{{ raid.stage }}</div><div class="raid-copy"><strong>{{ raid.name }}</strong><p>Level {{ raid.requiredLevel }} · {{ raid.maxHp }} HP · +{{ raid.xpReward }} XP</p><div class="boss-hp"><span :style="{ width: `${raid.unlocked ? 72 : 28}%` }"></span></div></div><button v-if="raid.unlocked && !clearedRaidIds.has(raid.id)" class="raid-button" :disabled="actionPending" @click="attemptRaid(raid)">Enter raid</button><span v-else :class="['raid-state', clearedRaidIds.has(raid.id) ? 'cleared' : 'locked']">{{ clearedRaidIds.has(raid.id) ? 'CLEARED' : 'LOCKED' }}</span></div></div>
          </section>
        </template>

        <footer>{{ authEnabled ? `Keycloak · ${authLabel}` : 'Development mode · temporary dev-user identity' }}</footer>
      </div>

      <div v-if="goalModalOpen" class="modal-backdrop" role="presentation" @click.self="closeGoalModal">
        <section class="goal-modal" role="dialog" aria-modal="true" aria-labelledby="goal-modal-title">
          <div class="panel-heading"><div><p class="eyebrow">GOAL SETUP</p><h2 id="goal-modal-title">{{ editingGoalId ? '목표 수정' : '새 목표 설정' }}</h2></div><button class="icon-button" type="button" aria-label="Close goal popup" @click="closeGoalModal">×</button></div>
          <form class="goal-form" @submit.prevent="saveGoalFromModal">
            <label>Goal title<input v-model="goalDraft.title" placeholder="예: QuestLog 출시, SQLP 자격증, 체력 강화" maxlength="200" /></label>
            <label>Description<input v-model="goalDraft.description" placeholder="목표 설명을 입력하면 직업과 퀘스트가 더 잘 맞춰집니다." /></label>
            <label>Target date<input v-model="goalDraft.targetDate" type="date" /></label>
            <div class="modal-preview" :class="`job-${deriveCharacterJob(goalDraft).key}`"><img :src="characterImageFor(goalDraft)" :alt="`${deriveCharacterJob(goalDraft).label} character preview`" /><div><strong>{{ deriveCharacterJob(goalDraft).label }}</strong><span>{{ deriveCharacterJob(goalDraft).subtitle }}</span></div></div>
            <div class="edit-actions modal-actions"><button type="submit" :disabled="actionPending || !goalDraft.title.trim()">{{ editingGoalId ? 'Save goal' : 'Create goal + auto quests' }}</button><button type="button" class="secondary-button" @click="closeGoalModal">Cancel</button></div>
          </form>
        </section>
      </div>
    </v-main>
  </v-app>
</template>
