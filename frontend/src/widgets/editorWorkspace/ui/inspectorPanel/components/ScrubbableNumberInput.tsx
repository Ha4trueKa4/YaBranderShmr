import { Input } from '@shared/ui'
import { MoveHorizontal } from 'lucide-react'
import { type PointerEvent, useRef } from 'react'

import styles from '../Properties.module.css'

interface ScrubbableNumberInputProps {
  axis?: 'horizontal' | 'vertical'
  min?: number
  onValueChange: (value: number) => void
  step?: number
  value: number
}

type DragState = {
  pointerId: number
  startPosition: number
  startValue: number
}

function roundToStep(value: number, step: number) {
  return Math.round(value / step) * step
}

function formatValue(value: number) {
  return Math.round(value * 100) / 100
}

export function ScrubbableNumberInput({
  axis = 'horizontal',
  min,
  onValueChange,
  step = 0.5,
  value,
}: ScrubbableNumberInputProps) {
  const dragStateRef = useRef<DragState | null>(null)

  function handleScrubPointerDown(event: PointerEvent<HTMLSpanElement>) {
    if (event.button !== 0) {
      return
    }

    dragStateRef.current = {
      pointerId: event.pointerId,
      startPosition: axis === 'horizontal' ? event.clientX : event.clientY,
      startValue: value,
    }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  function handleScrubPointerMove(event: PointerEvent<HTMLSpanElement>) {
    const dragState = dragStateRef.current

    if (!dragState || dragState.pointerId !== event.pointerId) {
      return
    }

    const currentPosition =
      axis === 'horizontal' ? event.clientX : event.clientY
    const offset =
      axis === 'horizontal'
        ? currentPosition - dragState.startPosition
        : currentPosition - dragState.startPosition
    const nextValue = roundToStep(dragState.startValue + offset * step, step)

    onValueChange(min === undefined ? nextValue : Math.max(min, nextValue))
  }

  function handleScrubPointerUp(event: PointerEvent<HTMLSpanElement>) {
    if (dragStateRef.current?.pointerId !== event.pointerId) {
      return
    }

    dragStateRef.current = null
    event.currentTarget.releasePointerCapture(event.pointerId)
  }

  function handleInputChange(nextValue: string) {
    const parsedValue = Number(nextValue)

    if (!Number.isFinite(parsedValue)) {
      return
    }

    onValueChange(min === undefined ? parsedValue : Math.max(min, parsedValue))
  }

  return (
    <Input
      className={styles.scrubField}
      leadingContent={
        <span
          className={
            axis === 'horizontal'
              ? styles.scrubHandle
              : `${styles.scrubHandle} ${styles.scrubHandleVertical}`
          }
          onPointerDown={handleScrubPointerDown}
          onPointerMove={handleScrubPointerMove}
          onPointerUp={handleScrubPointerUp}
        >
          <MoveHorizontal
            aria-hidden="true"
            className={
              axis === 'vertical' ? styles.scrubIconVertical : undefined
            }
            size={16}
          />
        </span>
      }
      min={min}
      onChange={(event) => handleInputChange(event.target.value)}
      step={step}
      type="number"
      value={formatValue(value)}
    />
  )
}
