import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useMemo,
  useState,
} from 'react'
import { ToastViewport } from './ToastViewport'
import type {
  Toast,
  ToastApi,
  ToastId,
  ToastMessage,
  ToastVariant,
} from './toast.types'

const maxToasts = 4
let nextToastId = 0

export const ToastContext = createContext<ToastApi | null>(null)

function createToastId() {
  nextToastId += 1

  return `toast-${nextToastId}`
}

export function ToastProvider({ children }: PropsWithChildren) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const dismiss = useCallback((id: ToastId) => {
    setToasts((currentToasts) =>
      currentToasts.filter((toast) => toast.id !== id),
    )
  }, [])

  const dismissAll = useCallback(() => setToasts([]), [])

  const show = useCallback((variant: ToastVariant, message: ToastMessage) => {
    const messages = Array.isArray(message) ? message : [message]
    const newToasts = messages.map((toastMessage) => ({
      id: createToastId(),
      message: toastMessage,
      variant,
    }))

    setToasts((currentToasts) =>
      [...currentToasts, ...newToasts].slice(-maxToasts),
    )

    return Array.isArray(message)
      ? newToasts.map((toast) => toast.id)
      : newToasts[0].id
  }, [])

  const value = useMemo<ToastApi>(
    () => ({
      dismiss,
      dismissAll,
      error: (message) => show('error', message),
      info: (message) => show('info', message),
    }),
    [dismiss, dismissAll, show],
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport onDismiss={dismiss} toasts={toasts} />
    </ToastContext.Provider>
  )
}
