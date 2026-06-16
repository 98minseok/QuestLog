import { describe, expect, it } from 'vitest'
import { buildWeeklyQuests, deriveCharacterJob } from './questPersona'

describe('deriveCharacterJob', () => {
  it.each([
    ['Study Korean grammar', 'Daily reading drills', 'Scholar'],
    ['Build a coding portfolio', 'Ship software every week', 'Code Mage'],
    ['Improve fitness', 'Run and strength training', 'Guardian'],
    ['Grow business revenue', 'Improve sales pipeline', 'Strategist'],
  ])('maps "%s" goals to %s', (title, description, expectedLabel) => {
    expect(deriveCharacterJob({ title, description }).label).toBe(expectedLabel)
  })

  it('falls back to Pathfinder when no goal context matches', () => {
    expect(deriveCharacterJob({ title: 'Organize life', description: null }).label).toBe(
      'Pathfinder',
    )
  })
})

describe('buildWeeklyQuests', () => {
  it('creates clearly labeled generated weekly quests for the active goal', () => {
    const quests = buildWeeklyQuests({ id: 3, title: 'Study Korean' })

    expect(quests).toHaveLength(2)
    expect(quests[0]).toMatchObject({
      id: 'weekly-3-checkpoint',
      goalId: 3,
      cadence: 'WEEKLY',
      title: '[WEEKLY] Set a milestone for Study Korean',
    })
    expect(quests.every((quest) => quest.xpReward > 0)).toBe(true)
  })
})
