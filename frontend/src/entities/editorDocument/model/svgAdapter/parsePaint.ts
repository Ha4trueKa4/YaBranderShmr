import type { Paint } from '../types'

const gradientReference = /^url\(#([^()\s]+)\)$/

export function parsePaint(value: string | null, fallback: Paint): Paint {
  if (value === null) {
    return fallback
  }

  if (value === 'none') {
    return { type: 'none' }
  }

  const gradientMatch = value.match(gradientReference)

  if (gradientMatch) {
    return { gradientId: gradientMatch[1], type: 'gradient' }
  }

  return { type: 'color', value }
}
