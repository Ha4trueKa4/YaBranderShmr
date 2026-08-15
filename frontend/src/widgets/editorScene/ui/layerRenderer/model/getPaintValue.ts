import type { Paint } from '@entities/editorDocument'

export function getPaintValue(paint: Paint): string {
  switch (paint.type) {
    case 'color':
      return paint.value
    case 'gradient':
      return `url(#${paint.gradientId})`
    case 'none':
      return 'none'
  }
}
