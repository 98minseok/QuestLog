import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'
import { archiveGoalRequest, completeWeeklyQuestRequest, fetchDashboard, type Dashboard } from './dashboardApi'

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
})
