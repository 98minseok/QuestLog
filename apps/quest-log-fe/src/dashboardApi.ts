import axios from 'axios'

export type TaskStatus = 'COMPLETED' | 'PENDING' | 'SKIPPED'
export type TaskSource = 'AI_RECOMMENDED' | 'MANUAL'

export type Goal = {
  id: number
  title: string
  description: string | null
  status: string
  targetDate: string | null
}

export type DailyTask = {
  id: number
  goalId: number | null
  title: string
  description: string | null
  taskDate: string
  status: TaskStatus
  source: TaskSource
  xpReward: number
}

export type CharacterProfile = {
  displayName: string
  level: number
  totalXp: number
  currentLevelXp: number
  xpToNextLevel: number
  strength: number
  vitality: number
}

export type BossRaid = {
  id: number
  stage: number
  name: string
  requiredLevel: number
  maxHp: number
  xpReward: number
  unlocked: boolean
}

export type RaidAttempt = {
  id: number
  bossRaidId: number
  bossName: string
  stage: number
  status: string
  damageDealt: number
}

export type Dashboard = {
  taskDate: string
  goals: Goal[]
  dailyTasks: DailyTask[]
  character: CharacterProfile
  raids: BossRaid[]
  raidAttempts: RaidAttempt[]
}

export function taskUpdatePayload(
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

export async function fetchDashboard(taskDate: string) {
  const response = await axios.get<Dashboard>('/api/bff/dashboard', {
    params: { taskDate },
  })
  return response.data
}

export async function archiveGoalRequest(goal: Goal) {
  await axios.put(`/api/bff/goals/${goal.id}`, {
    title: goal.title,
    description: goal.description,
    status: 'ARCHIVED',
    targetDate: goal.targetDate,
  })
}

export async function skipTaskRequest(task: DailyTask) {
  await axios.put(
    `/api/bff/daily-tasks/${task.id}`,
    taskUpdatePayload(task, { status: 'SKIPPED' }),
  )
}

export async function deleteTaskRequest(taskId: number) {
  await axios.delete(`/api/bff/daily-tasks/${taskId}`)
}

export async function completeTaskRequest(taskId: number) {
  const response = await axios.post<{ xpAwarded: number }>(
    `/api/bff/daily-tasks/${taskId}/complete`,
  )
  return response.data.xpAwarded
}
