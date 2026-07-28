import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'
import {
  acceptDailyRecommendationsRequest,
  archiveGoalRequest,
  attackRaidAttemptRequest,
  completeWeeklyQuestRequest,
  createDailyTaskRequest,
  createWeeklyQuestRequest,
  fetchDashboard,
  fetchRecommendationHistoryRequest,
  previewDailyRecommendationsRequest,
  resolveRaidAttemptRequest,
  startRaidAttemptRequest,
  type Dashboard,
} from './dashboardApi'

vi.mock('./dashboardApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./dashboardApi')>()
  return {
    ...actual,
    archiveGoalRequest: vi.fn(),
    attackRaidAttemptRequest: vi.fn(),
    completeWeeklyQuestRequest: vi.fn(),
    completeTaskRequest: vi.fn(),
    createDailyTaskRequest: vi.fn(),
    createWeeklyQuestRequest: vi.fn(),
    deleteWeeklyQuestRequest: vi.fn(),
    deleteTaskRequest: vi.fn(),
    fetchDashboard: vi.fn(),
    fetchRecommendationHistoryRequest: vi.fn(),
    acceptDailyRecommendationsRequest: vi.fn(),
    previewDailyRecommendationsRequest: vi.fn(),
    resolveRaidAttemptRequest: vi.fn(),
    skipWeeklyQuestRequest: vi.fn(),
    skipTaskRequest: vi.fn(),
    startRaidAttemptRequest: vi.fn(),
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
  goalProgressSummaries: [
    {
      goalId: 7,
      dailyQuestCount: 1,
      weeklyQuestCount: 1,
      completedQuestCount: 1,
      pendingQuestCount: 1,
      skippedQuestCount: 0,
      earnedXp: 25,
      availableXp: 100,
      completionRate: 50,
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
  progressionEvents: [
    {
      id: 21,
      userId: 1,
      sourceType: 'WEEKLY_QUEST',
      sourceId: 17,
      xpAwarded: 75,
      totalXp: 120,
      level: 2,
      strength: 2,
      vitality: 2,
      createdAt: '2026-06-15T09:00:00Z',
    },
  ],
  raids: [],
  raidAttempts: [],
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(fetchDashboard).mockResolvedValue(dashboard)
  vi.mocked(fetchRecommendationHistoryRequest).mockResolvedValue([])
  vi.mocked(archiveGoalRequest).mockResolvedValue()
  vi.mocked(completeWeeklyQuestRequest).mockResolvedValue(75)
  vi.mocked(startRaidAttemptRequest).mockResolvedValue({
    id: 51,
    bossRaidId: 3,
    bossName: 'Slime Sovereign',
    stage: 1,
    status: 'STARTED',
    damageDealt: 0,
    bossRemainingHp: 100,
  })
  vi.mocked(attackRaidAttemptRequest).mockResolvedValue({
    attemptId: 51,
    bossRaidId: 3,
    bossName: 'Slime Sovereign',
    stage: 1,
    status: 'CLEARED',
    damageDealt: 100,
    bossRemainingHp: 0,
    xpAwarded: 50,
  })
  vi.mocked(resolveRaidAttemptRequest).mockResolvedValue({
    attemptId: 51,
    bossRaidId: 3,
    bossName: 'Slime Sovereign',
    stage: 1,
    status: 'FAILED',
    damageDealt: 70,
    bossRemainingHp: 30,
    xpAwarded: 0,
  })
  vi.mocked(createDailyTaskRequest).mockResolvedValue({
    id: 31,
    goalId: 7,
    title: 'Draft release notes',
    description: 'Summarize completed work',
    taskDate: '2026-06-15',
    status: 'PENDING',
    source: 'MANUAL',
    xpReward: 25,
  })
  vi.mocked(createWeeklyQuestRequest).mockResolvedValue({
    id: 43,
    goalId: 7,
    title: 'Publish the weekly demo build',
    description: 'Cut a working build for review',
    weekStartDate: '2026-06-15',
    status: 'PENDING',
    source: 'MANUAL',
    xpReward: 125,
  })
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
    expect(wrapper.text()).toContain('XP LOG')
    expect(wrapper.text()).toContain('WEEKLY QUEST')
    expect(wrapper.text()).toContain('COMPLETION')
    expect(wrapper.text()).toContain('ROUTE PROGRESS')
    expect(wrapper.text()).toContain('25/100')

    const routeProgress = wrapper.find('[aria-label="Selected goal quest completion"]')
    expect(routeProgress.exists()).toBe(true)
    expect(routeProgress.find('[role="meter"]').attributes('aria-valuenow')).toBe('50')
  })

  it('reloads dashboard data when the quest date changes', async () => {
    const wrapper = mount(App)
    await flushPromises()

    await wrapper.find('input[aria-label="Dashboard quest date"]').setValue('2026-06-16')
    await flushPromises()

    expect(fetchDashboard).toHaveBeenLastCalledWith('2026-06-16')
    expect(wrapper.text()).toContain('2026-06-16 / WEEK OF 2026-06-15')
    expect(wrapper.text()).toContain('Selected day quests')
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

  it('creates a manual daily quest for the selected goal', async () => {
    const wrapper = mount(App)
    await flushPromises()

    await wrapper.find('input[aria-label="Dashboard quest date"]').setValue('2026-06-16')
    await flushPromises()

    const addButton = wrapper.findAll('button').find((button) => button.text() === 'Add daily quest')
    expect(addButton).toBeDefined()
    await addButton!.trigger('click')

    await wrapper.find('input[aria-label="New daily quest title"]').setValue('Draft release notes')
    await wrapper.find('input[aria-label="New daily quest description"]').setValue('Summarize completed work')
    await wrapper.find('input[aria-label="New daily quest XP reward"]').setValue(25)
    await wrapper.find('form.manual-task-composer').trigger('submit')
    await flushPromises()

    expect(createDailyTaskRequest).toHaveBeenCalledWith({
      goalId: 7,
      title: 'Draft release notes',
      description: 'Summarize completed work',
      taskDate: '2026-06-16',
      xpReward: 25,
    })
    expect(fetchDashboard).toHaveBeenCalledTimes(3)
    expect(wrapper.text()).toContain('Manual daily quest added.')
  })

  it('creates a manual weekly quest for the selected goal and dashboard week', async () => {
    const wrapper = mount(App)
    await flushPromises()

    await wrapper.find('input[aria-label="Dashboard quest date"]').setValue('2026-06-18')
    await flushPromises()

    const addButton = wrapper.findAll('button').find((button) => button.text() === 'Add weekly quest')
    expect(addButton).toBeDefined()
    await addButton!.trigger('click')

    await wrapper.find('input[aria-label="New weekly quest title"]').setValue('Publish the weekly demo build')
    await wrapper.find('input[aria-label="New weekly quest description"]').setValue('Cut a working build for review')
    await wrapper.find('input[aria-label="New weekly quest XP reward"]').setValue(125)
    await wrapper.find('form.weekly-composer').trigger('submit')
    await flushPromises()

    expect(createWeeklyQuestRequest).toHaveBeenCalledWith({
      goalId: 7,
      title: 'Publish the weekly demo build',
      description: 'Cut a working build for review',
      weekStartDate: '2026-06-15',
      xpReward: 125,
    })
    expect(fetchDashboard).toHaveBeenCalledTimes(3)
    expect(wrapper.text()).toContain('Manual weekly quest added.')
  })

  it('renders active raid progress and advances it with the staged attack helper', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue({
      ...dashboard,
      raids: [
        {
          id: 3,
          stage: 1,
          name: 'Slime Sovereign',
          requiredLevel: 1,
          maxHp: 100,
          xpReward: 50,
          unlocked: true,
        },
      ],
      raidAttempts: [
        {
          id: 51,
          bossRaidId: 3,
          bossName: 'Slime Sovereign',
          stage: 1,
          status: 'IN_PROGRESS',
          damageDealt: 70,
          bossRemainingHp: 30,
        },
      ],
    })
    const wrapper = mount(App)
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === 'Boss raid')!.trigger('click')
    expect(wrapper.text()).toContain('30 HP remains / 70 damage dealt')

    await wrapper.findAll('button').find((button) => button.text() === 'Attack')!.trigger('click')
    await flushPromises()

    expect(attackRaidAttemptRequest).toHaveBeenCalledWith(51)
    expect(fetchDashboard).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Slime Sovereign cleared. +50 XP')
  })

  it('renders recent raid attempt history with outcomes and combat totals', async () => {
    vi.mocked(fetchDashboard).mockResolvedValue({
      ...dashboard,
      raids: [
        {
          id: 3,
          stage: 1,
          name: 'Slime Sovereign',
          requiredLevel: 1,
          maxHp: 100,
          xpReward: 50,
          unlocked: true,
        },
      ],
      raidAttempts: [
        {
          id: 51,
          bossRaidId: 3,
          bossName: 'Slime Sovereign',
          stage: 1,
          status: 'CLEARED',
          damageDealt: 100,
          bossRemainingHp: 0,
          startedAt: '2026-06-15T09:00:00Z',
          completedAt: '2026-06-15T09:03:00Z',
        },
        {
          id: 52,
          bossRaidId: 3,
          bossName: 'Slime Sovereign',
          stage: 1,
          status: 'FAILED',
          damageDealt: 70,
          bossRemainingHp: 30,
          startedAt: '2026-06-14T09:00:00Z',
          completedAt: '2026-06-14T09:02:00Z',
        },
      ],
    })
    const wrapper = mount(App)
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === 'Boss raid')!.trigger('click')

    expect(wrapper.text()).toContain('RAID HISTORY')
    expect(wrapper.text()).toContain('2 logged')
    expect(wrapper.text()).toContain('Stage 1 / Slime Sovereign')
    expect(wrapper.text()).toContain('CLEARED')
    expect(wrapper.text()).toContain('WITHDRAWN')
    expect(wrapper.text()).toContain('100 damage / 0 HP left')
    expect(wrapper.text()).toContain('70 damage / 30 HP left')
  })

  it('previews, edits, rejects, and accepts selected daily quest recommendations', async () => {
    vi.mocked(fetchRecommendationHistoryRequest).mockResolvedValue([
      {
        id: 41,
        goalId: 7,
        createdTaskId: 31,
        provider: 'deterministic-mock',
        action: 'ACCEPTED',
        title: 'Rehearse the QuestLog launch demo',
        description: 'Practice the edited pitch flow',
        taskDate: '2026-06-15',
        source: 'AI_RECOMMENDED',
        xpReward: 35,
        createdAt: '2026-06-15T09:00:00Z',
      },
    ])
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

    expect(fetchRecommendationHistoryRequest).toHaveBeenCalledWith(7, 6)
    expect(wrapper.text()).toContain('AI ACTIVITY')
    expect(wrapper.text()).toContain('Rehearse the QuestLog launch demo')

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
