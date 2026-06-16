import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  archiveGoalRequest,
  completeTaskRequest,
  createGoalRequest,
  deleteTaskRequest,
  fetchDashboard,
  skipTaskRequest,
  taskUpdatePayload,
  type DailyTask,
  type Goal,
} from './dashboardApi'

vi.mock('axios', () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

const mockedAxios = vi.mocked(axios)

const goal: Goal = {
  id: 7,
  title: 'Ship QuestLog',
  description: 'Finish the dashboard',
  status: 'ACTIVE',
  targetDate: '2026-07-01',
}

const task: DailyTask = {
  id: 11,
  goalId: 7,
  title: 'Add lifecycle tests',
  description: 'Cover dashboard actions',
  taskDate: '2026-06-15',
  status: 'PENDING',
  source: 'MANUAL',
  xpReward: 25,
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('dashboard API lifecycle requests', () => {
  it('loads the dashboard for the requested date', async () => {
    const dashboard = { goals: [], dailyTasks: [] }
    mockedAxios.get.mockResolvedValue({ data: dashboard })

    await expect(fetchDashboard('2026-06-15')).resolves.toBe(dashboard)
    expect(mockedAxios.get).toHaveBeenCalledWith('/api/bff/dashboard', {
      params: { taskDate: '2026-06-15' },
    })
  })

  it('archives a goal while preserving its editable fields', async () => {
    mockedAxios.put.mockResolvedValue({})

    await archiveGoalRequest(goal)

    expect(mockedAxios.put).toHaveBeenCalledWith('/api/bff/goals/7', {
      title: goal.title,
      description: goal.description,
      status: 'ARCHIVED',
      targetDate: goal.targetDate,
    })
  })

  it('creates a goal with the task date used for automatic recommendations', async () => {
    mockedAxios.post.mockResolvedValue({ data: goal })

    await expect(
      createGoalRequest({
        title: goal.title,
        description: goal.description ?? '',
        targetDate: goal.targetDate,
        taskDate: '2026-06-16',
      }),
    ).resolves.toBe(goal)

    expect(mockedAxios.post).toHaveBeenCalledWith('/api/bff/goals', {
      title: goal.title,
      description: goal.description,
      targetDate: goal.targetDate,
      taskDate: '2026-06-16',
    })
  })

  it('skips a task while preserving the complete update payload', async () => {
    mockedAxios.put.mockResolvedValue({})

    await skipTaskRequest(task)

    expect(mockedAxios.put).toHaveBeenCalledWith('/api/bff/daily-tasks/11', {
      goalId: task.goalId,
      title: task.title,
      description: task.description,
      taskDate: task.taskDate,
      status: 'SKIPPED',
      xpReward: task.xpReward,
    })
  })

  it('deletes only the requested task resource', async () => {
    mockedAxios.delete.mockResolvedValue({})

    await deleteTaskRequest(task.id)

    expect(mockedAxios.delete).toHaveBeenCalledWith('/api/bff/daily-tasks/11')
  })

  it('returns awarded XP from task completion', async () => {
    mockedAxios.post.mockResolvedValue({ data: { xpAwarded: 25 } })

    await expect(completeTaskRequest(task.id)).resolves.toBe(25)
    expect(mockedAxios.post).toHaveBeenCalledWith('/api/bff/daily-tasks/11/complete')
  })
})

describe('taskUpdatePayload', () => {
  it('preserves lifecycle state unless an update explicitly replaces it', () => {
    expect(taskUpdatePayload(task, { title: 'Renamed task' })).toEqual({
      goalId: task.goalId,
      title: 'Renamed task',
      description: task.description,
      taskDate: task.taskDate,
      status: 'PENDING',
      xpReward: task.xpReward,
    })
  })
})
