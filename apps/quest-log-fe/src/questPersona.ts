import type { Goal } from './dashboardApi'

export type CharacterJobKey = 'scholar' | 'code-mage' | 'guardian' | 'strategist' | 'pathfinder'

export type CharacterJob = {
  key: CharacterJobKey
  label: string
  subtitle: string
}

export type WeeklyQuest = {
  id: string
  goalId: number
  title: string
  description: string
  xpReward: number
  cadence: 'WEEKLY'
}

const JOBS: Record<CharacterJobKey, CharacterJob> = {
  scholar: {
    key: 'scholar',
    label: 'Scholar',
    subtitle: 'Knowledge-focused growth',
  },
  'code-mage': {
    key: 'code-mage',
    label: 'Code Mage',
    subtitle: 'Systems and software mastery',
  },
  guardian: {
    key: 'guardian',
    label: 'Guardian',
    subtitle: 'Strength, health, and discipline',
  },
  strategist: {
    key: 'strategist',
    label: 'Strategist',
    subtitle: 'Money, business, and planning',
  },
  pathfinder: {
    key: 'pathfinder',
    label: 'Pathfinder',
    subtitle: 'Balanced long-term progress',
  },
}

const KEYWORDS: Array<{ key: CharacterJobKey; words: string[] }> = [
  {
    key: 'scholar',
    words: ['study', 'learn', 'school', 'exam', 'book', 'read', 'language', 'research', 'certificate', 'sqlp', '공부', '학습', '시험', '독서', '영어', '자격증'],
  },
  {
    key: 'code-mage',
    words: ['code', 'coding', 'program', 'software', 'developer', 'app', 'ship', 'debug', '코딩', '개발', '앱', '프로젝트', '출시'],
  },
  {
    key: 'guardian',
    words: ['fitness', 'health', 'workout', 'run', 'running', 'gym', 'diet', 'strength', '운동', '헬스', '건강', '체력', '다이어트'],
  },
  {
    key: 'strategist',
    words: ['money', 'business', 'startup', 'sales', 'invest', 'finance', 'budget', 'career', '돈', '사업', '투자', '재테크', '매출', '커리어'],
  },
]

export function deriveCharacterJob(goal: Pick<Goal, 'title' | 'description'> | null | undefined) {
  if (!goal) return JOBS.pathfinder

  const haystack = `${goal.title} ${goal.description ?? ''}`.toLowerCase()
  const match = KEYWORDS.find(({ words }) =>
    words.some((word) =>
      /[a-z0-9]/i.test(word) ? new RegExp(`\\b${word}\\b`).test(haystack) : haystack.includes(word),
    ),
  )
  return JOBS[match?.key ?? 'pathfinder']
}

export function buildWeeklyQuests(goal: Pick<Goal, 'id' | 'title'> | null | undefined): WeeklyQuest[] {
  if (!goal) return []

  return [
    {
      id: `weekly-${goal.id}-checkpoint`,
      goalId: goal.id,
      title: `[WEEKLY] Set a milestone for ${goal.title}`,
      description: 'Choose one measurable result to finish before the week ends.',
      xpReward: 40,
      cadence: 'WEEKLY',
    },
    {
      id: `weekly-${goal.id}-review`,
      goalId: goal.id,
      title: `[WEEKLY] Review progress on ${goal.title}`,
      description: 'Summarize wins, blockers, and the next weekly adjustment.',
      xpReward: 30,
      cadence: 'WEEKLY',
    },
  ]
}
