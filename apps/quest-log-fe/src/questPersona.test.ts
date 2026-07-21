import { describe, expect, it } from 'vitest'
import { deriveCharacterJob } from './questPersona'

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
