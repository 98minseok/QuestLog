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
} as const

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
  status: string
  source: string
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

const activeGoals = computed(() =>
  goals.value.filter((goal) => goal.status === GOAL_STATUS.active),
)
const completedTaskCount = computed(
  () => tasks.value.filter((task) => task.status === TASK_STATUS.completed).length,
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

async function deletePendingTask(taskId: number) {
  await runAction(
    async () => {
      await axios.delete(`/api/bff/daily-tasks/${taskId}`)
    },
    'Daily task deleted.',
  )
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
                      class="text-button muted"
                      :disabled="actionPending || goal.status !== GOAL_STATUS.active"
                      @click="archiveGoal(goal)"
                    >
                      Archive
                    </button>
                  </div>
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
                  v-for="task in tasks"
                  :key="task.id"
                  class="task-item"
                  :class="{ completed: task.status === TASK_STATUS.completed }"
                >
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
                    <span>{{ task.source === 'AI_RECOMMENDED' ? 'MOCK AI' : 'MANUAL' }}</span>
                  </div>
                  <b>+{{ task.xpReward }} XP</b>
                  <button
                    v-if="task.status === TASK_STATUS.pending"
                    class="task-delete-button"
                    :disabled="actionPending"
                    :aria-label="`Delete ${task.title}`"
                    @click="deletePendingTask(task.id)"
                  >
                    Delete
                  </button>
                </div>
                <p v-if="tasks.length === 0" class="empty-copy">
                  No quests for today. Add one or ask for a recommendation.
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
