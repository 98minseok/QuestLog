import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'
import {
  acceptDailyRecommendationsRequest,
  archiveGoalRequest,
  completeWeeklyQuestRequest,
  fetchDashboard,
  previewDailyRecommendationsRequest,
  type Dashboard,
} from './dashboardApi'

vi.mock('./dashboardApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./dashboardApi')>()
  return {
    ...actual,
    archiveGoalRequest: vi.fn(),
    completeWeeklyQuestRequest: vi.fn(),
    completeTaskRequest: vi.fn(),
    deleteWeeklyQuestRequest: vi.fn(),
    deleteTaskRequest: vi.fn(),
    fetchDashboard: vi.fn(),
    acceptDailyRecommendationsRequest: vi.fn(),
    previewDailyRecommendationsRequest: vi.fn(),
    skipWeeklyQuestRequest: vi.fn(),
    skipTaskRequest: vi.fn(),
  }
})

const dashboard: Dashboard = {
  taskDate: '2026-06-15',
  goals: [
    {
      id: 7,
      title: 'Ship QuestLog',
      description: 'Finish the dashboard',
      status: 'ACTIVE',
      targetDate: null,
    },
  ],
  dailyTasks: [],
  weeklyQuests: [
    {
      id: 17,
      goalId: 7,
      title: 'Review shipping progress',
      description: 'Summarize blockers and next moves',
      weekStartDate: '2026-06-15',
      status: 'PENDING',
      source: 'SYSTEM',
      xpReward: 75,
    },
  ],
  character: {
    displayName: 'Test Hero',
    level: 2,
    totalXp: 120,
    currentLevelXp: 20,
    xpToNextLevel: 80,
    strength: 2,
    vitality: 2,
  },
  raids: [],
  raidAttempts: [],
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(fetchDashboard).mockResolvedValue(dashboard)
  vi.mocked(archiveGoalRequest).mockResolvedValue()
  vi.mocked(completeWeeklyQuestRequest).mockResolvedValue(75)
  vi.mocked(acceptDailyRecommendationsRequest).mockResolvedValue([])
  vi.mocked(previewDailyRecommendationsRequest).mockResolvedValue([])
  vi.spyOn(window, 'confirm').mockReturnValue(true)
})

describe('App dashboard lifecycle', () => {
  it('loads dashboard data and renders a basic component smoke view', async () => {
    const wrapper = mount(App)
    await flushPromises()

    expect(fetchDashboard).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Ship QuestLog')
    expect(wrapper.text()).toContain('Test Hero')
  })

  it('archives a goal after confirmation and refreshes the dashboard', async () => {
    const wrapper = mount(App)
    await flushPromises()

    const archiveButton = wrapper.findAll('button').find((button) => button.text() === 'Archive')
    expect(archiveButton).toBeDefined()
    await archiveButton!.trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenCalledWith('Archive "Ship QuestLog"?')
    expect(archiveGoalRequest).toHaveBeenCalledWith(dashboard.goals[0])
    expect(fetchDashboard).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Goal archived.')
  })

  it('renders persisted weekly quests and completes them through the BFF helper', async () => {
    const wrapper = mount(App)
    await flushPromises()

    expect(wrapper.text()).toContain('Review shipping progress')
    const completeButton = wrapper.find('button[aria-label="Complete Review shipping progress"]')
    expect(completeButton.exists()).toBe(true)
    await completeButton.trigger('click')
    await flushPromises()

    expect(completeWeeklyQuestRequest).toHaveBeenCalledWith(17)
    expect(fetchDashboard).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Weekly quest complete. +75 XP')
  })

  it('previews, edits, rejects, and accepts selected daily quest recommendations', async () => {
    vi.mocked(previewDailyRecommendationsRequest).mockResolvedValue([
      {
        goalId: 7,
        title: 'Plan the next step for Ship QuestLog',
        description: 'Write one concrete outcome',
        taskDate: '2026-06-15',
        source: 'AI_RECOMMENDED',
        xpReward: 10,
      },
      {
        goalId: 7,
        title: 'Focus on Ship QuestLog for 25 minutes',
        description: 'Complete one uninterrupted focus session',
        taskDate: '2026-06-15',
        source: 'AI_RECOMMENDED',
        xpReward: 20,
      },
    ])
    vi.mocked(acceptDailyRecommendationsRequest).mockResolvedValue([
      {
        id: 31,
        goalId: 7,
        title: 'Rehearse the QuestLog launch demo',
        description: 'Practice the edited pitch flow',
        taskDate: '2026-06-15',
        status: 'PENDING',
        source: 'AI_RECOMMENDED',
        xpReward: 35,
      },
    ])
    const wrapper = mount(App)
    await flushPromises()

    const generateButton = wrapper.findAll('button').find((button) => button.text() === 'Generate Quests')
    expect(generateButton).toBeDefined()
    await generateButton!.trigger('click')
    await flushPromises()

    expect(previewDailyRecommendationsRequest).toHaveBeenCalledWith(7, expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/))
    expect(acceptDailyRecommendationsRequest).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('SYSTEM DAILY DRAFT')

    const recommendationTitles = wrapper.findAll('input[aria-label="Recommendation title"]')
    await recommendationTitles[0].setValue('Rehearse the QuestLog launch demo')
    const recommendationDescriptions = wrapper.findAll('input[aria-label="Recommendation description"]')
    await recommendationDescriptions[0].setValue('Practice the edited pitch flow')
    const recommendationXp = wrapper.findAll('input[aria-label="Recommendation XP reward"]')
    await recommendationXp[0].setValue(35)
    const rejectButtons = wrapper.findAll('button').filter((button) => button.text() === 'Reject')
    expect(rejectButtons).toHaveLength(2)
    await rejectButtons[1].trigger('click')

    const acceptButton = wrapper.findAll('button').find((button) => button.text() === 'Accept selected')
    expect(acceptButton).toBeDefined()
    await acceptButton!.trigger('click')
    await flushPromises()

    expect(acceptDailyRecommendationsRequest).toHaveBeenCalledWith(7, [
      {
        goalId: 7,
        title: 'Rehearse the QuestLog launch demo',
        description: 'Practice the edited pitch flow',
        taskDate: '2026-06-15',
        source: 'AI_RECOMMENDED',
        xpReward: 35,
      },
    ])
    expect(fetchDashboard).toHaveBeenCalledTimes(3)
    expect(wrapper.text()).toContain('1 recommended daily quest is ready.')
  })
})
