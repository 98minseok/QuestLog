import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'
import { archiveGoalRequest, fetchDashboard } from './dashboardApi'

vi.mock('./dashboardApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./dashboardApi')>()
  return {
    ...actual,
    archiveGoalRequest: vi.fn(),
    completeTaskRequest: vi.fn(),
    deleteTaskRequest: vi.fn(),
    fetchDashboard: vi.fn(),
    skipTaskRequest: vi.fn(),
  }
})

const dashboard = {
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
  weeklyQuests: [],
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
})
