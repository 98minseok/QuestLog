import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  acceptDailyRecommendationsRequest,
  archiveGoalRequest,
  completeWeeklyQuestRequest,
  completeTaskRequest,
  createDailyTaskRequest,
  createGoalRequest,
  deleteTaskRequest,
  deleteWeeklyQuestRequest,
  fetchDashboard,
  fetchRecommendationHistoryRequest,
  previewDailyRecommendationsRequest,
  recommendDailyTasksRequest,
  skipTaskRequest,
  skipWeeklyQuestRequest,
  taskUpdatePayload,
  type DailyTask,
  type Goal,
  type WeeklyQuest,
  weeklyQuestUpdatePayload,
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

const weeklyQuest: WeeklyQuest = {
  id: 17,
  goalId: 7,
  title: 'Review shipping progress',
  description: 'Summarize blockers and next moves',
  weekStartDate: '2026-06-15',
  status: 'PENDING',
  source: 'SYSTEM',
  xpReward: 75,
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

  it('requests daily recommendations for an existing goal and task date', async () => {
    mockedAxios.post.mockResolvedValue({ data: [task] })

    await expect(recommendDailyTasksRequest(goal.id, '2026-06-16')).resolves.toEqual([task])
    expect(mockedAxios.post).toHaveBeenCalledWith(
      '/api/bff/goals/7/recommendations',
      undefined,
      { params: { taskDate: '2026-06-16' } },
    )
  })

  it('creates a manual daily task for the requested goal and date', async () => {
    mockedAxios.post.mockResolvedValue({ data: task })

    await expect(createDailyTaskRequest({
      goalId: goal.id,
      title: 'Add lifecycle tests',
      description: 'Cover dashboard actions',
      taskDate: '2026-06-16',
      xpReward: 25,
    })).resolves.toBe(task)

    expect(mockedAxios.post).toHaveBeenCalledWith('/api/bff/daily-tasks', {
      goalId: goal.id,
      title: 'Add lifecycle tests',
      description: 'Cover dashboard actions',
      taskDate: '2026-06-16',
      xpReward: 25,
    })
  })

  it('previews daily recommendation drafts without creating tasks', async () => {
    const draft = {
      goalId: goal.id,
      title: 'Plan the next step for Ship QuestLog',
      description: 'Write one concrete outcome.',
      taskDate: '2026-06-16',
      xpReward: 10,
      source: 'AI_RECOMMENDED' as const,
    }
    mockedAxios.get.mockResolvedValue({ data: [draft] })

    await expect(previewDailyRecommendationsRequest(goal.id, '2026-06-16')).resolves.toEqual([draft])
    expect(mockedAxios.get).toHaveBeenCalledWith(
      '/api/bff/goals/7/recommendations/preview',
      { params: { taskDate: '2026-06-16' } },
    )
  })

  it('accepts edited recommendation drafts through the BFF accept endpoint', async () => {
    const draft = {
      goalId: goal.id,
      title: 'Rehearse the three-minute demo',
      description: 'Practice the edited pitch flow.',
      taskDate: '2026-06-16',
      xpReward: 35,
      source: 'AI_RECOMMENDED' as const,
    }
    mockedAxios.post.mockResolvedValue({ data: [task] })

    await expect(acceptDailyRecommendationsRequest(goal.id, [draft])).resolves.toEqual([task])
    expect(mockedAxios.post).toHaveBeenCalledWith(
      '/api/bff/goals/7/recommendations/accept',
      [
        {
          title: 'Rehearse the three-minute demo',
          description: 'Practice the edited pitch flow.',
          taskDate: '2026-06-16',
          xpReward: 35,
        },
      ],
    )
  })

  it('loads recent recommendation history for a goal', async () => {
    const history = [
      {
        id: 41,
        goalId: goal.id,
        createdTaskId: task.id,
        provider: 'deterministic-mock',
        action: 'ACCEPTED' as const,
        title: 'Rehearse the three-minute demo',
        description: 'Practice the edited pitch flow.',
        taskDate: '2026-06-16',
        xpReward: 35,
        source: 'AI_RECOMMENDED' as const,
        createdAt: '2026-06-16T09:00:00Z',
      },
    ]
    mockedAxios.get.mockResolvedValue({ data: history })

    await expect(fetchRecommendationHistoryRequest(goal.id, 5)).resolves.toEqual(history)
    expect(mockedAxios.get).toHaveBeenCalledWith(
      '/api/bff/goals/7/recommendations/history',
      { params: { limit: 5 } },
    )
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

  it('returns awarded XP from weekly quest completion', async () => {
    mockedAxios.post.mockResolvedValue({ data: { xpAwarded: 75 } })

    await expect(completeWeeklyQuestRequest(17)).resolves.toBe(75)
    expect(mockedAxios.post).toHaveBeenCalledWith('/api/bff/weekly-quests/17/complete')
  })

  it('skips a weekly quest while preserving the complete update payload', async () => {
    mockedAxios.put.mockResolvedValue({})

    await skipWeeklyQuestRequest(weeklyQuest)

    expect(mockedAxios.put).toHaveBeenCalledWith('/api/bff/weekly-quests/17', {
      goalId: weeklyQuest.goalId,
      title: weeklyQuest.title,
      description: weeklyQuest.description,
      weekStartDate: weeklyQuest.weekStartDate,
      status: 'SKIPPED',
      xpReward: weeklyQuest.xpReward,
    })
  })

  it('deletes only the requested weekly quest resource', async () => {
    mockedAxios.delete.mockResolvedValue({})

    await deleteWeeklyQuestRequest(weeklyQuest.id)

    expect(mockedAxios.delete).toHaveBeenCalledWith('/api/bff/weekly-quests/17')
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

describe('weeklyQuestUpdatePayload', () => {
  it('preserves weekly quest lifecycle state unless an update explicitly replaces it', () => {
    expect(weeklyQuestUpdatePayload(weeklyQuest, { title: 'Renamed weekly quest' })).toEqual({
      goalId: weeklyQuest.goalId,
      title: 'Renamed weekly quest',
      description: weeklyQuest.description,
      weekStartDate: weeklyQuest.weekStartDate,
      status: 'PENDING',
      xpReward: weeklyQuest.xpReward,
    })
  })
})
