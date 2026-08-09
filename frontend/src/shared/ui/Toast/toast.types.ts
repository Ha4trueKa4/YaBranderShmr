export type ToastId = string

export type ToastMessage = string | string[]

export type ToastVariant = 'error' | 'info'

export interface Toast {
  id: ToastId
  message: string
  variant: ToastVariant
}

export interface ToastApi {
  dismiss: (id: ToastId) => void
  dismissAll: () => void
  error: (message: ToastMessage) => ToastId | ToastId[]
  info: (message: ToastMessage) => ToastId | ToastId[]
}
