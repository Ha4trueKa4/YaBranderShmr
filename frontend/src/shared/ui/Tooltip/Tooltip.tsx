import {
  type CSSProperties,
  cloneElement,
  type FocusEventHandler,
  type MouseEventHandler,
  type ReactElement,
  useCallback,
  useLayoutEffect,
  useState,
} from 'react'
import { Portal } from '../Portal'
import styles from './Tooltip.module.css'

interface TooltipTriggerProps {
  onBlur?: FocusEventHandler<HTMLElement>
  onFocus?: FocusEventHandler<HTMLElement>
  onMouseEnter?: MouseEventHandler<HTMLElement>
  onMouseLeave?: MouseEventHandler<HTMLElement>
}

export interface TooltipProps {
  children: ReactElement<TooltipTriggerProps>
  content: string
}

export function Tooltip({ children, content }: TooltipProps) {
  const [trigger, setTrigger] = useState<HTMLElement | null>(null)
  const [position, setPosition] = useState<CSSProperties>()
  const isOpen = trigger !== null

  const updatePosition = useCallback(() => {
    if (!trigger) {
      return
    }

    const { bottom, left, width } = trigger.getBoundingClientRect()

    setPosition({
      left: left + width / 2,
      top: bottom + 8,
    })
  }, [trigger])

  useLayoutEffect(() => {
    if (!isOpen) {
      return
    }

    updatePosition()
    window.addEventListener('resize', updatePosition)
    window.addEventListener('scroll', updatePosition, true)

    return () => {
      window.removeEventListener('resize', updatePosition)
      window.removeEventListener('scroll', updatePosition, true)
    }
  }, [isOpen, updatePosition])

  return (
    <>
      {cloneElement(children, {
        onBlur: (event) => {
          children.props.onBlur?.(event)
          setTrigger(null)
        },
        onFocus: (event) => {
          children.props.onFocus?.(event)
          setTrigger(event.currentTarget)
        },
        onMouseEnter: (event) => {
          children.props.onMouseEnter?.(event)
          setTrigger(event.currentTarget)
        },
        onMouseLeave: (event) => {
          children.props.onMouseLeave?.(event)
          setTrigger(null)
        },
      })}
      {isOpen ? (
        <Portal>
          <span className={styles.tooltip} style={position}>
            {content}
          </span>
        </Portal>
      ) : null}
    </>
  )
}
