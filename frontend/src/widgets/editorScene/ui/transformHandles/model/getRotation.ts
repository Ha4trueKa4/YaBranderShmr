type PointerPosition = { x: number; y: number }

function normalizeAngle(angle: number) {
  return ((((angle + 180) % 360) + 360) % 360) - 180
}

function round(angle: number) {
  return Math.round(angle * 100) / 100
}

export function getPointerAngle(
  center: PointerPosition,
  pointer: PointerPosition,
) {
  return (
    (Math.atan2(pointer.y - center.y, pointer.x - center.x) * 180) / Math.PI
  )
}

export function getRotation(
  initialRotation: number,
  startPointerAngle: number,
  currentPointerAngle: number,
) {
  return round(
    normalizeAngle(
      initialRotation + normalizeAngle(currentPointerAngle - startPointerAngle),
    ),
  )
}
