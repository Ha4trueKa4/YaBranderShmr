import type { Transform } from '@entities/editorDocument'

export function getTransformValue(transform: Transform): string | undefined {
  const operations: string[] = []

  if (transform.x !== 0 || transform.y !== 0) {
    operations.push(`translate(${transform.x} ${transform.y})`)
  }

  if (transform.rotation !== 0) {
    const rotationCenter = transform.rotationCenter
    const rotation = rotationCenter
      ? `${transform.rotation} ${rotationCenter.x} ${rotationCenter.y}`
      : String(transform.rotation)

    operations.push(`rotate(${rotation})`)
  }

  if (transform.scaleX !== 1 || transform.scaleY !== 1) {
    operations.push(`scale(${transform.scaleX} ${transform.scaleY})`)
  }

  return operations.length > 0 ? operations.join(' ') : undefined
}
