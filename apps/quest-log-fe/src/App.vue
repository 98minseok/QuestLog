<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'

const GOAL_STATUS = {
  active: 'ACTIVE',
  archived: 'ARCHIVED',
} as const

const TASK_STATUS = {
  completed: 'COMPLETED',
  pending: 'PENDING',
  skipped: 'SKIPPED',
} as const

const TASK_FILTERS = ['ALL', ...Object.values(TASK_STATUS)] as const

type TaskStatus = (typeof TASK_STATUS)[keyof typeof TASK_STATUS]
type TaskFilter = (typeof TASK_FILTERS)[number]
type TaskSource = 'AI_RECOMMENDED' | 'MANUAL'

type Goal = {
  id: number
  title: string
  description: string | null
  status: string
  targetDate: string | null
}

type DailyTask = {
  id: number
  goalId: number | null
  title: string
  description: string | null
  taskDate: string
  status: TaskStatus
  source: TaskSource
  xpReward: number
}

type CharacterProfile = {
  displayName: string
  level: number
  totalXp: number
  currentLevelXp: number
  xpToNextLevel: number
  strength: number
  vitality: number
}

type BossRaid = {
  id: number
  stage: number
  name: string
  requiredLevel: number
  maxHp: number
  xpReward: number
  unlocked: boolean
}

type RaidAttempt = {
  id: number
  bossRaidId: number
  bossName: string
  stage: number
  status: string
  damageDealt: number
}

type Dashboard = {
  taskDate: string
  goals: Goal[]
  dailyTasks: DailyTask[]
  character: CharacterProfile
  raids: BossRaid[]
  raidAttempts: RaidAttempt[]
}

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
const newGoalTitle = ref('')
const newTaskTitle = ref('')
const selectedGoalId = ref<number | null>(null)
const taskFilter = ref<TaskFilter>('ALL')
const editingGoalId = ref<number | null>(null)
const goalDraft = ref({
  title: '',
  description: '',
  targetDate: '',
})
const editingTaskId = ref<number | null>(null)
const taskDraft = ref({
  title: '',
  description: '',
  goalId: null as number | null,
  xpReward: 10,
})

const activeGoals = computed(() =>
  goals.value.filter((goal) => goal.status === GOAL_STATUS.active),
)
const taskGoalOptions = computed(() =>
  goals.value.filter(
    (goal) => goal.status === GOAL_STATUS.active || goal.id === taskDraft.value.goalId,
  ),
)
const completedTaskCount = computed(
  () => tasks.value.filter((task) => task.status === TASK_STATUS.completed).length,
)
const filteredTasks = computed(() =>
  taskFilter.value === 'ALL'
    ? tasks.value
    : tasks.value.filter((task) => task.status === taskFilter.value),
)
const filteredTasksEmptyMessage = computed(() =>
  tasks.value.length === 0
    ? 'No quests for today. Add one or ask for a recommendation.'
    : `No ${taskFilter.value.toLowerCase()} quests for today.`,
)
const progressPercent = computed(() => character.value?.currentLevelXp ?? 0)
const clearedRaidIds = computed(
  () =>
    new Set(
      attempts.value
        .filter((attempt) => attempt.status === 'VICTORY')
        .map((attempt) => attempt.bossRaidId),
    ),
)

function apiMessage(caught: unknown) {
  if (axios.isAxiosError(caught)) {
    return caught.response?.data?.message ?? 'The QuestLog backend is unavailable.'
  }
  return 'An unexpected error occurred.'
}

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const response = await axios.get<Dashboard>('/api/bff/dashboard', {
      params: { taskDate: today },
    })
    goals.value = response.data.goals
    tasks.value = response.data.dailyTasks
    character.value = response.data.character
    raids.value = response.data.raids
    attempts.value = response.data.raidAttempts
    if (selectedGoalId.value === null && activeGoals.value.length > 0) {
      selectedGoalId.value = activeGoals.value[0]?.id ?? null
    }
  } catch (caught) {
    error.value = apiMessage(caught)
  } finally {
    loading.value = false
  }
}

async function createGoal() {
  if (!newGoalTitle.value.trim()) return
  await runAction(
    async () => {
      const response = await axios.post<Goal>('/api/bff/goals', {
        title: newGoalTitle.value,
        description: 'Created from the dashboard',
      })
      newGoalTitle.value = ''
      selectedGoalId.value = response.data.id
    },
    'Goal added.',
  )
}

async function createTask() {
  if (!newTaskTitle.value.trim()) return
  await runAction(
    async () => {
      await axios.post('/api/bff/daily-tasks', {
        goalId: selectedGoalId.value,
        title: newTaskTitle.value,
        description: 'Created from the dashboard',
        taskDate: today,
        xpReward: 10,
      })
      newTaskTitle.value = ''
    },
    'Daily task added.',
  )
}

async function archiveGoal(goal: Goal) {
  await runAction(
    async () => {
      await axios.put(`/api/bff/goals/${goal.id}`, {
        title: goal.title,
        description: goal.description,
        status: GOAL_STATUS.archived,
        targetDate: goal.targetDate,
      })
      if (selectedGoalId.value === goal.id) {
        selectedGoalId.value = null
      }
    },
    'Goal archived.',
  )
}

async function confirmArchiveGoal(goal: Goal) {
  await confirmAction(`Archive "${goal.title}"?`, () => archiveGoal(goal))
}

function startGoalEdit(goal: Goal) {
  editingGoalId.value = goal.id
  goalDraft.value = {
    title: goal.title,
    description: goal.description ?? '',
    targetDate: goal.targetDate ?? '',
  }
}

function cancelGoalEdit() {
  editingGoalId.value = null
}

async function saveGoal(goal: Goal) {
  if (!goalDraft.value.title.trim()) return
  await runAction(
    async () => {
      await axios.put(`/api/bff/goals/${goal.id}`, {
        title: goalDraft.value.title,
        description: goalDraft.value.description,
        status: goal.status,
        targetDate: goalDraft.value.targetDate || null,
      })
      editingGoalId.value = null
    },
    'Goal updated.',
  )
}

async function recommend(goalId: number) {
  await runAction(
    async () => {
      await axios.post(`/api/bff/goals/${goalId}/recommendations`, null, {
        params: { taskDate: today },
      })
    },
    'Mock AI suggestions are ready for today.',
  )
}

function startTaskEdit(task: DailyTask) {
  editingTaskId.value = task.id
  taskDraft.value = {
    title: task.title,
    description: task.description ?? '',
    goalId: task.goalId,
    xpReward: task.xpReward,
  }
}

function cancelTaskEdit() {
  editingTaskId.value = null
}

function isPendingTask(task: DailyTask) {
  return task.status === TASK_STATUS.pending
}

function taskSourceLabel(task: DailyTask) {
  if (task.status === TASK_STATUS.skipped) return 'SKIPPED'
  return task.source === 'AI_RECOMMENDED' ? 'MOCK AI' : 'MANUAL'
}

async function saveTask(task: DailyTask) {
  if (
    !taskDraft.value.title.trim() ||
    !Number.isInteger(taskDraft.value.xpReward) ||
    taskDraft.value.xpReward < 1 ||
    taskDraft.value.xpReward > 1000
  ) {
    return
  }
  await runAction(
    async () => {
      await axios.put(`/api/bff/daily-tasks/${task.id}`, taskUpdatePayload(task, {
        goalId: taskDraft.value.goalId,
        title: taskDraft.value.title,
        description: taskDraft.value.description,
        xpReward: taskDraft.value.xpReward,
      }))
      editingTaskId.value = null
    },
    'Daily task updated.',
  )
}

async function completeTask(task: DailyTask) {
  await runAction(
    async () => {
      const response = await axios.post<{ xpAwarded: number }>(
        `/api/bff/daily-tasks/${task.id}/complete`,
      )
      return response.data.xpAwarded
    },
    (xpAwarded) => `Quest complete. +${xpAwarded} XP`,
  )
}

async function skipPendingTask(task: DailyTask) {
  await runAction(
    async () => {
      await axios.put(`/api/bff/daily-tasks/${task.id}`, taskUpdatePayload(task, {
        status: TASK_STATUS.skipped,
      }))
    },
    'Daily task skipped.',
  )
}

function taskUpdatePayload(
  task: DailyTask,
  updates: Partial<
    Pick<DailyTask, 'goalId' | 'title' | 'description' | 'status' | 'xpReward'>
  >,
) {
  return {
    goalId: task.goalId,
    title: task.title,
    description: task.description,
    taskDate: task.taskDate,
    status: task.status,
    xpReward: task.xpReward,
    ...updates,
  }
}

async function deletePendingTask(taskId: number) {
  await runAction(
    async () => {
      await axios.delete(`/api/bff/daily-tasks/${taskId}`)
    },
    'Daily task deleted.',
  )
}

async function confirmDeletePendingTask(task: DailyTask) {
  await confirmAction(`Delete "${task.title}"?`, () => deletePendingTask(task.id))
}

async function attemptRaid(raid: BossRaid) {
  await runAction(
    async () => {
      const response = await axios.post<{ xpAwarded: number }>(
        `/api/bff/boss-raids/${raid.id}/attempts`,
      )
      return response.data.xpAwarded
    },
    (xpAwarded) => `${raid.name} cleared. +${xpAwarded} XP`,
  )
}

async function runAction<T>(
  action: () => Promise<T>,
  successMessage: string | ((result: T) => string),
) {
  actionPending.value = true
  error.value = ''
  notice.value = ''
  try {
    const result = await action()
    notice.value =
      typeof successMessage === 'function' ? successMessage(result) : successMessage
    await loadDashboard()
  } catch (caught) {
    error.value = apiMessage(caught)
  } finally {
    actionPending.value = false
  }
}

async function confirmAction(message: string, action: () => Promise<void>) {
  if (window.confirm(message)) {
    await action()
  }
}

onMounted(loadDashboard)
</script>

<template>
  <v-app>
    <v-main>
      <div class="dashboard-shell">
        <header class="hero-panel">
          <div>
            <p class="eyebrow">QUESTLOG / DEV HERO</p>
            <h1>Turn today’s work into progress.</h1>
            <p class="hero-copy">
              Build goals, clear daily quests, earn XP, and unlock the next raid.
            </p>
          </div>
          <div class="hero-stats">
            <div>
              <span>ACTIVE GOALS</span>
              <strong>{{ activeGoals.length }}</strong>
            </div>
            <div>
              <span>TODAY CLEARED</span>
              <strong>{{ completedTaskCount }}/{{ tasks.length }}</strong>
            </div>
          </div>
        </header>

        <v-alert v-if="error" type="error" variant="tonal" closable class="mb-4">
          {{ error }}
        </v-alert>
        <v-alert v-if="notice" type="success" variant="tonal" closable class="mb-4">
          {{ notice }}
        </v-alert>

        <div v-if="loading" class="loading-grid">
          <v-skeleton-loader v-for="index in 4" :key="index" type="heading, paragraph, actions" />
        </div>

        <template v-else>
          <section class="dashboard-grid">
            <article class="panel goals-panel">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">LONG-TERM QUESTS</p>
                  <h2>Goals</h2>
                </div>
                <span class="count">{{ goals.length }}</span>
              </div>

              <form class="inline-form" @submit.prevent="createGoal">
                <input v-model="newGoalTitle" placeholder="Add a new goal" maxlength="200" />
                <button :disabled="actionPending || !newGoalTitle.trim()">Add</button>
              </form>

              <div class="item-list">
                <div v-for="goal in goals" :key="goal.id" class="goal-item">
                  <form
                    v-if="editingGoalId === goal.id"
                    class="edit-form"
                    @submit.prevent="saveGoal(goal)"
                  >
                    <input v-model="goalDraft.title" maxlength="200" aria-label="Goal title" />
                    <input v-model="goalDraft.description" aria-label="Goal description" />
                    <input
                      v-model="goalDraft.targetDate"
                      type="date"
                      aria-label="Goal target date"
                    />
                    <div class="edit-actions">
                      <button
                        type="submit"
                        :disabled="actionPending || !goalDraft.title.trim()"
                      >
                        Save
                      </button>
                      <button type="button" class="secondary-button" @click="cancelGoalEdit">
                        Cancel
                      </button>
                    </div>
                  </form>
                  <template v-else>
                    <div>
                      <span class="status-dot" :class="goal.status.toLowerCase()" />
                      <strong>{{ goal.title }}</strong>
                      <p>{{ goal.description || 'No description yet.' }}</p>
                    </div>
                    <div class="item-actions">
                      <button
                        class="text-button"
                        :disabled="actionPending || goal.status !== GOAL_STATUS.active"
                        @click="recommend(goal.id)"
                      >
                        Suggest quests
                      </button>
                      <button
                        class="text-button"
                        :disabled="actionPending || goal.status !== GOAL_STATUS.active"
                        @click="startGoalEdit(goal)"
                      >
                        Edit
                      </button>
                      <button
                        class="text-button muted"
                        :disabled="actionPending || goal.status !== GOAL_STATUS.active"
                        @click="confirmArchiveGoal(goal)"
                      >
                        Archive
                      </button>
                    </div>
                  </template>
                </div>
                <p v-if="goals.length === 0" class="empty-copy">
                  Create your first long-term quest to unlock daily recommendations.
                </p>
              </div>
            </article>

            <article class="panel character-panel">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">CHARACTER</p>
                  <h2>{{ character?.displayName ?? 'Quest Hero' }}</h2>
                </div>
                <span class="level-badge">LV {{ character?.level ?? 1 }}</span>
              </div>

              <div class="avatar-orbit">
                <div class="avatar-core">Q</div>
              </div>

              <div class="xp-block">
                <div class="xp-label">
                  <span>XP PROGRESS</span>
                  <strong>{{ character?.currentLevelXp ?? 0 }} / 100</strong>
                </div>
                <v-progress-linear
                  :model-value="progressPercent"
                  color="#e7ff54"
                  bg-color="#29312f"
                  height="10"
                  rounded
                />
              </div>

              <div class="stat-row">
                <div><span>Strength</span><strong>{{ character?.strength ?? 1 }}</strong></div>
                <div><span>Vitality</span><strong>{{ character?.vitality ?? 1 }}</strong></div>
                <div><span>Total XP</span><strong>{{ character?.totalXp ?? 0 }}</strong></div>
              </div>
            </article>

            <article class="panel tasks-panel">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">{{ today }}</p>
                  <h2>Daily quests</h2>
                </div>
                <span class="count">{{ tasks.length }}</span>
              </div>

              <div class="filter-bar" aria-label="Daily task status filter">
                <button
                  v-for="filter in TASK_FILTERS"
                  :key="filter"
                  type="button"
                  :class="{ active: taskFilter === filter }"
                  :aria-pressed="taskFilter === filter"
                  @click="taskFilter = filter"
                >
                  {{ filter }}
                </button>
              </div>

              <form class="task-form" @submit.prevent="createTask">
                <input v-model="newTaskTitle" placeholder="Add today’s task" maxlength="200" />
                <select v-model="selectedGoalId">
                  <option :value="null">No goal</option>
                  <option v-for="goal in activeGoals" :key="goal.id" :value="goal.id">
                    {{ goal.title }}
                  </option>
                </select>
                <button :disabled="actionPending || !newTaskTitle.trim()">Add quest</button>
              </form>

              <div class="task-list">
                <div
                  v-for="task in filteredTasks"
                  :key="task.id"
                  class="task-item"
                  :class="{
                    completed: task.status === TASK_STATUS.completed,
                    skipped: task.status === TASK_STATUS.skipped,
                  }"
                >
                  <form
                    v-if="editingTaskId === task.id"
                    class="edit-form task-edit-form"
                    @submit.prevent="saveTask(task)"
                  >
                    <input v-model="taskDraft.title" maxlength="200" aria-label="Task title" />
                    <input v-model="taskDraft.description" aria-label="Task description" />
                    <select v-model="taskDraft.goalId" aria-label="Task goal">
                      <option :value="null">No goal</option>
                      <option v-for="goal in taskGoalOptions" :key="goal.id" :value="goal.id">
                        {{ goal.title }}
                      </option>
                    </select>
                    <input
                      v-model.number="taskDraft.xpReward"
                      type="number"
                      min="1"
                      max="1000"
                      aria-label="Task XP reward"
                    />
                    <div class="edit-actions">
                      <button
                        type="submit"
                        :disabled="
                          actionPending ||
                          !taskDraft.title.trim() ||
                          !Number.isInteger(taskDraft.xpReward) ||
                          taskDraft.xpReward < 1 ||
                          taskDraft.xpReward > 1000
                        "
                      >
                        Save
                      </button>
                      <button type="button" class="secondary-button" @click="cancelTaskEdit">
                        Cancel
                      </button>
                    </div>
                  </form>
                  <template v-else>
                    <button
                      class="complete-button"
                      :disabled="actionPending || task.status !== TASK_STATUS.pending"
                      :aria-label="`Complete ${task.title}`"
                      @click="completeTask(task)"
                    >
                      <span v-if="task.status === TASK_STATUS.completed">✓</span>
                    </button>
                    <div class="task-copy">
                      <strong>{{ task.title }}</strong>
                      <span>{{ taskSourceLabel(task) }}</span>
                    </div>
                    <b>+{{ task.xpReward }} XP</b>
                    <button
                      v-if="isPendingTask(task)"
                      class="text-button muted"
                      :disabled="actionPending"
                      @click="skipPendingTask(task)"
                    >
                      Skip
                    </button>
                    <button
                      v-if="isPendingTask(task)"
                      class="text-button"
                      :disabled="actionPending"
                      @click="startTaskEdit(task)"
                    >
                      Edit
                    </button>
                    <button
                      v-if="isPendingTask(task)"
                      class="task-delete-button"
                      :disabled="actionPending"
                      :aria-label="`Delete ${task.title}`"
                      @click="confirmDeletePendingTask(task)"
                    >
                      Delete
                    </button>
                  </template>
                </div>
                <p v-if="filteredTasks.length === 0" class="empty-copy">
                  {{ filteredTasksEmptyMessage }}
                </p>
              </div>
            </article>

            <article class="panel raids-panel">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">BOSS PATH</p>
                  <h2>Raids</h2>
                </div>
                <span class="count">{{ attempts.length }} attempts</span>
              </div>

              <div class="raid-list">
                <div v-for="raid in raids" :key="raid.id" class="raid-card">
                  <div class="stage">0{{ raid.stage }}</div>
                  <div>
                    <strong>{{ raid.name }}</strong>
                    <p>Level {{ raid.requiredLevel }} · {{ raid.maxHp }} HP · +{{ raid.xpReward }} XP</p>
                  </div>
                  <button
                    v-if="raid.unlocked && !clearedRaidIds.has(raid.id)"
                    class="raid-button"
                    :disabled="actionPending"
                    @click="attemptRaid(raid)"
                  >
                    Clear raid
                  </button>
                  <span
                    v-else
                    :class="[
                      'raid-state',
                      clearedRaidIds.has(raid.id) ? 'cleared' : 'locked',
                    ]"
                  >
                    {{ clearedRaidIds.has(raid.id) ? 'CLEARED' : 'LOCKED' }}
                  </span>
                </div>
              </div>
            </article>
          </section>
        </template>

        <footer>
          Development mode uses the temporary <code>dev-user</code> backend identity.
        </footer>
      </div>
    </v-main>
  </v-app>
</template>
